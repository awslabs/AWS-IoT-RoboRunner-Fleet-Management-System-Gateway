/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.iotroborunner.fmsg.translations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.constants.RoboRunnerWorkerStatusConstants;
import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the MiR FMS response translator module. */
@ExtendWith(MockitoExtension.class)
public class MirFmsResponseTranslatorTest {
    @Mock
    private PositionTranslation mockPositionTranslation;

    @Mock
    private OrientationTranslation mockOrientationTranslation;

    /** String represenattion of MiR FMS GET robots response. */
    private String fmsResponse;

    private final ObjectMapper mapper = new ObjectMapper();

    private MirFmsResponseTranslator translator;

    /**
     * Setup mock PositionTranslation for use in tests.
     */
    void setupMockPositionTranslation() {
        final double[] rrCoords = {4.5, 3.2};
        when(mockPositionTranslation.getRoboRunnerCoordinatesFromFmsCoordinates(anyDouble(), anyDouble()))
            .thenReturn(rrCoords);
    }

    /**
     * Setup mock OrientationTranslation for use in tests.
     */
    void setupMockOrientationTranslation() {
        when(mockOrientationTranslation.getRoboRunnerOrientationFromFmsOrientation(anyDouble()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Set up mocks required in the tests.
     */
    @BeforeEach
    public void setup() throws IOException {
        fmsResponse = Files.readString(
            Paths.get("tst/com/amazon/iotroborunner/fmsg/translations/mir_response.json"),
                      StandardCharsets.US_ASCII);
        assertTrue(StringUtils.isNotBlank(fmsResponse));

        translator = new MirFmsResponseTranslator();
    }

    @Test
    void given_validMirResponseJson_when_getWorkerStatusFromFmsResponse_then_passes() throws JsonProcessingException {
        setupMockPositionTranslation();
        setupMockOrientationTranslation();

        final WorkerStatus status = translator.getWorkerStatusFromFmsResponse(
            TestConstants.MIR_ROBOT_ID, fmsResponse, mockPositionTranslation, mockOrientationTranslation
        );
        assertNotNull(status);
        
        final Map<String, Object> workerTransientProps = mapper.readValue(
            status.getWorkerAdditionalTransientProperties(),
            new TypeReference<HashMap<String, Object>>() {}
        );
        assertEquals(0.01, (Double) workerTransientProps.get(RoboRunnerWorkerStatusConstants.BATTERY_LEVEL));
        assertEquals(4.5, status.getPosition().getCartesianCoordinates().getX());
        assertEquals(3.2, status.getPosition().getCartesianCoordinates().getY());
        assertEquals(-0.4605743885040283, status.getOrientation().getDegrees());
        final Map<String, Object> vendorTransientProps = mapper.readValue(
            status.getVendorProperties().getVendorAdditionalTransientProperties(),
            new TypeReference<HashMap<String, Object>>() {}
        );
        assertEquals("Error", vendorTransientProps.get(RoboRunnerWorkerStatusConstants.VENDOR_STATE));
        final Map<String, Double> vendorPosition = (Map) vendorTransientProps
            .get(RoboRunnerWorkerStatusConstants.VENDOR_POSITION);
        assertEquals(22.377197265625, vendorPosition.get(RoboRunnerWorkerStatusConstants.VENDOR_X));
        assertEquals(10.37197494506836, vendorPosition.get(RoboRunnerWorkerStatusConstants.VENDOR_Y));
        final Map<String, Double> vendorOrientation = (Map) vendorTransientProps
            .get(RoboRunnerWorkerStatusConstants.VENDOR_ORIENTATION); 
        assertEquals(-0.4605743885040283 + 360.0, vendorOrientation.get(RoboRunnerWorkerStatusConstants.DEGREES));
    }

    @Test
    void given_invalidJson_when_getWorkerStatusFromFmsResponse_then_throwsJsonProcessingException() {
        assertThrows(JsonProcessingException.class, () -> {
            translator.getWorkerStatusFromFmsResponse(
                TestConstants.MIR_ROBOT_ID, "invalid JSON", mockPositionTranslation, mockOrientationTranslation);
        });
    }

    @Test
    void given_nullPositionTranslation_when_getWorkerStatusFromFmsResponse_then_workerPositionSetToNull()
            throws JsonProcessingException {
        setupMockOrientationTranslation();
        final WorkerStatus status = translator.getWorkerStatusFromFmsResponse(
            TestConstants.MIR_ROBOT_ID, fmsResponse, null, mockOrientationTranslation);

        assertNotNull(status);
        assertNull(status.getPosition());
        assertNotNull(status.getOrientation());
    }

    @Test
    void given_nullOrientationTranslation_when_getWorkerStatusFromFmsResponse_then_workerOrientationSetToNull()
            throws JsonProcessingException {
        setupMockPositionTranslation();
        final WorkerStatus status = translator.getWorkerStatusFromFmsResponse(
            TestConstants.MIR_ROBOT_ID, fmsResponse, mockPositionTranslation, null);

        assertNotNull(status);
        assertNull(status.getOrientation());
        assertNotNull(status.getPosition());
    }
}
