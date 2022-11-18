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

package com.amazon.iotroborunner.fmsg.utils;

import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.SHARED_SPACE_ARN;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.OUT_OF_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.WAITING_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.DESTINATION_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.SITE_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_FLEET_ARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the utility methods for Simulated FMS connector module.
 */
@ExtendWith(MockitoExtension.class)
public class SimulatedFmsConnectorUtilsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Logger logger;
    private static MockedAppender mockedAppender;

    /**
     * Set up logger required in the tests.
     */
    @BeforeAll
    public static void setUpLogger() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(SimulatedFmsConnectorUtils.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
    }

    /**
     * Clean up the used logger test resources.
     */
    @AfterAll
    public static void cleanUp() {
        if (mockedAppender != null) {
            logger.removeAppender(mockedAppender);
        }
    }

    /**
     * Clean up the used mocked appender test resources.
     */
    @AfterEach
    public void clearLogHistory() {
        mockedAppender.clear();
    }

    @Test
    void given_workerIsWaitingForSharedSpace_when_isWorkerWaitingForSharedSpace_then_returnsTrue() {
        final boolean actualResult = SimulatedFmsConnectorUtils.isWorkerWaitingForSharedSpace(
                WAITING_FOR_SHARED_SPACE.value);

        assertTrue(actualResult);
    }

    @Test
    void given_workerIsOutOfSharedSpace_when_isWorkerWaitingForSharedSpace_then_returnsFalse() {
        final boolean actualResult = SimulatedFmsConnectorUtils.isWorkerWaitingForSharedSpace(
                OUT_OF_SHARED_SPACE.value);

        assertFalse(actualResult);
    }

    @Test
    void given_workerIsWaitingForSharedSpace_when_isWorkerOutOfSharedSpace_then_returnsFalse() {
        final boolean actualResult = SimulatedFmsConnectorUtils.isWorkerOutOfSharedSpace(
                WAITING_FOR_SHARED_SPACE.value);

        assertFalse(actualResult);
    }

    @Test
    void given_workerIsOutOfSharedSpace_when_isWorkerOutOfSharedSpace_then_returnsTrue() {
        final boolean actualResult = SimulatedFmsConnectorUtils.isWorkerOutOfSharedSpace(
                OUT_OF_SHARED_SPACE.value);

        assertTrue(actualResult);
    }

    @Test
    public void given_worker_when_getWorkerLocationStatusToSharedSpaceArnMapping_then_returnsTheMapping()
            throws JsonProcessingException {
        // Given
        final Map<String, String> customerTransientProperties = new HashMap<>() {{
                put(WORKER_LOCATION_STATUS.value, OUT_OF_SHARED_SPACE.value);
                put(SHARED_SPACE_ARN.value, DESTINATION_ARN);
            }};
        final WorkerAdditionalTransientProperties  addProps = new WorkerAdditionalTransientProperties();
        addProps.setCustomTransientProperties(customerTransientProperties);

        final Worker worker = new Worker()
                .withSite(SITE_ARN)
                .withFleet(WORKER_FLEET_ARN)
                .withAdditionalTransientProperties(OBJECT_MAPPER.writeValueAsString(addProps));

        // Testing
        final Pair<String, String> workerLocationStatus = SimulatedFmsConnectorUtils
                .getWorkerLocationStatusToSharedSpaceArnMapping(worker);

        // Verification
        assertEquals(OUT_OF_SHARED_SPACE.value, workerLocationStatus.getKey());
        assertEquals(DESTINATION_ARN, workerLocationStatus.getValue());
    }

    @Test
    public void given_workerWithInvalidAdditionalProps_when_getWorkerLocationStatusToSharedSpaceArnMapping_then_returnsNull() {
        final String invalidAdditionalPropertiesString = "invalidFormatStr";

        final Worker worker = new Worker()
                .withSite(SITE_ARN)
                .withFleet(WORKER_FLEET_ARN)
                .withAdditionalTransientProperties(invalidAdditionalPropertiesString);

        final Pair<String, String> workerLocationStatus = SimulatedFmsConnectorUtils
                    .getWorkerLocationStatusToSharedSpaceArnMapping(worker);

        assertNull(workerLocationStatus);
    }
}
