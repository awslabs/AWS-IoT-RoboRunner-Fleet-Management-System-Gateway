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

import static com.amazon.iotroborunner.fmsg.constants.RoboRunnerWorkerStatusConstants.JSON_SCHEMA_VERSION;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.SHARED_SPACE_ARN;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.IN_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.DESTINATION_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.ROBOT_IP;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.SITE_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_FLEET_ARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.testhelpers.TestUtils;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.roborunner.OrientationOffset;
import com.amazon.iotroborunner.fmsg.types.roborunner.PositionConversionCalibrationPoint;
import com.amazon.iotroborunner.fmsg.types.roborunner.ReferencePoint;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerFleetAdditionalFixedProperties;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.GetWorkerFleetRequest;
import com.amazonaws.services.iotroborunner.model.GetWorkerFleetResult;
import com.amazonaws.services.iotroborunner.model.ListWorkersRequest;
import com.amazonaws.services.iotroborunner.model.ListWorkersResult;
import com.amazonaws.services.iotroborunner.model.Orientation;
import com.amazonaws.services.iotroborunner.model.PositionCoordinates;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerRequest;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerResult;
import com.amazonaws.services.iotroborunner.model.VendorProperties;
import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the RoboRunner utilities module. */
@ExtendWith(MockitoExtension.class)
public class RoboRunnerUtilsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Mock
    private AWSIoTRoboRunner mockRrClient;

    @Mock
    private WorkerStatus mockWorkerStatus;

    @Mock
    private GetWorkerFleetResult mockWorkerFleetResult;

    @Mock
    private Worker worker1;

    @Mock
    private Worker worker2;

    @Mock
    private Worker worker3;

    private RoboRunnerUtils rrUtils;

    private final List<PositionConversionCalibrationPoint> positionConversion = Arrays.asList(
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(0.0).ycoordinate(0.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build(),
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(0.0).ycoordinate(149.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build(),
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(279.0).ycoordinate(0.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build(),
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(279.0).ycoordinate(149.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build()
        );

    private final OrientationOffset orientationOffset = OrientationOffset.builder().degrees(5.0).build();

    private final WorkerFleetAdditionalFixedProperties workerFleetProperties =
        new WorkerFleetAdditionalFixedProperties();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Setup mock WorkerStatus object.
     */
    public void setupWorkerStatusMocks() {
        when(mockWorkerStatus.getPosition()).thenReturn(mock(PositionCoordinates.class));
        when(mockWorkerStatus.getVendorProperties()).thenReturn(mock(VendorProperties.class));
        when(mockWorkerStatus.getOrientation()).thenReturn(mock(Orientation.class));
        when(mockWorkerStatus.getWorkerAdditionalTransientProperties()).thenReturn("");
    }

    /**
     * Setup WorkerFleet additionalFixedProperties object.
     */

    void setupWorkerFleetAdditionalFixedProperties() {
        workerFleetProperties.setSchemaVersion("1.0");
        workerFleetProperties.setPositionConversion(positionConversion);
        workerFleetProperties.setOrientationOffset(orientationOffset);

    }

    /**
     * Setup mocks and other dependencies for all unit tests.
     */
    @BeforeEach
    public void setUp() {
        rrUtils = new RoboRunnerUtils(mockRrClient);    
    }

    @Test
    void given_validParameters_when_updateRoboRunnerWorkerStatus_then_updateWorkerCalled()
            throws JsonProcessingException {
        setupWorkerStatusMocks();
        rrUtils.updateRoboRunnerWorkerStatus(WORKER_ARN, mockWorkerStatus);
        verify(mockRrClient, times(1)).updateWorker(any(UpdateWorkerRequest.class));
    }

    @Test
    void given_badWorkerStatus_whenUpdateRoboRunnerWorkerStatus_then_sdkExceptionThrown() {
        setupWorkerStatusMocks();
        when(mockRrClient.updateWorker(any(UpdateWorkerRequest.class))).thenThrow(SdkBaseException.class);

        assertThrows(SdkBaseException.class, () -> {
            rrUtils.updateRoboRunnerWorkerStatus(WORKER_ARN, mockWorkerStatus);
        });
    }

    @Test
    void given_worker_updateRoboRunnerWorkerAdditionalTransientProperties_then_workerUpdated()
            throws JsonProcessingException {
        final WorkerAdditionalTransientProperties addProps = new WorkerAdditionalTransientProperties();
        final Map<String, String> workerCustomTransientProperties = new HashMap<>() {{
                put(WORKER_LOCATION_STATUS.value, IN_SHARED_SPACE.value);
                put(SHARED_SPACE_ARN.value, DESTINATION_ARN);
            }};
        addProps.setCustomTransientProperties(workerCustomTransientProperties);
        addProps.setSchemaVersion(JSON_SCHEMA_VERSION);
        when(mockRrClient.updateWorker(
                new UpdateWorkerRequest()
                        .withId(WORKER_ARN)
                        .withAdditionalTransientProperties(OBJECT_MAPPER.writeValueAsString(addProps))))
                .thenReturn(new UpdateWorkerResult());

        rrUtils.updateRoboRunnerWorkerAdditionalTransientProperties(WORKER_ARN, addProps);
        verify(mockRrClient, times(1)).updateWorker(any(UpdateWorkerRequest.class));
    }

    @Test
    void given_validAdditionalInformationJson_when_getWorkerFleetAdditionalFixedProperties_then_returnsNotNull()
            throws JsonProcessingException {
        setupWorkerFleetAdditionalFixedProperties();
        when(mockRrClient.getWorkerFleet(any(GetWorkerFleetRequest.class))).thenReturn(mockWorkerFleetResult);
        when(mockWorkerFleetResult.getAdditionalFixedProperties())
            .thenReturn(mapper.writeValueAsString(workerFleetProperties));

        final Optional<WorkerFleetAdditionalFixedProperties> response =
            rrUtils.getWorkerFleetAdditionalFixedProperties(WORKER_FLEET_ARN);

        assertTrue(response.isPresent());
        assertEquals(this.positionConversion, response.get().getPositionConversion());
        assertEquals(this.orientationOffset, response.get().getOrientationOffset());
    }

    @Test
    void given_absentOrientationOffset_when_getWorkerFleetAdditionalFixedProperties_then_returnsValid()
            throws JsonProcessingException {
        workerFleetProperties.setSchemaVersion("1.0");
        workerFleetProperties.setPositionConversion(positionConversion);
        when(mockRrClient.getWorkerFleet(any(GetWorkerFleetRequest.class))).thenReturn(mockWorkerFleetResult);
        when(mockWorkerFleetResult.getAdditionalFixedProperties())
            .thenReturn(mapper.writeValueAsString(workerFleetProperties));

        final Optional<WorkerFleetAdditionalFixedProperties> response =
            rrUtils.getWorkerFleetAdditionalFixedProperties(WORKER_FLEET_ARN);

        assertTrue(response.isPresent());
        assertEquals(this.positionConversion, response.get().getPositionConversion());
        assertNull(response.get().getOrientationOffset());
    }

    @Test
    void given_absentPositionConversion_when_getWorkerFleetAdditionalFixedProperties_then_returnsValid()
            throws JsonProcessingException {
        workerFleetProperties.setSchemaVersion("1.0");
        workerFleetProperties.setOrientationOffset(orientationOffset);
        when(mockRrClient.getWorkerFleet(any(GetWorkerFleetRequest.class))).thenReturn(mockWorkerFleetResult);
        when(mockWorkerFleetResult.getAdditionalFixedProperties())
            .thenReturn(mapper.writeValueAsString(workerFleetProperties));

        final Optional<WorkerFleetAdditionalFixedProperties> response =
            rrUtils.getWorkerFleetAdditionalFixedProperties(WORKER_FLEET_ARN);

        assertTrue(response.isPresent());
        assertNull(response.get().getPositionConversion());
        assertEquals(this.orientationOffset, response.get().getOrientationOffset());
    }

    @Test
    void given_invalidAdditionalInformationJson_when_getWorkerFleetAdditionalFixedProperties_then_returnsEmpty() {
        when(mockRrClient.getWorkerFleet(any(GetWorkerFleetRequest.class))).thenReturn(mockWorkerFleetResult);
        when(mockWorkerFleetResult.getAdditionalFixedProperties()).thenReturn("invalid JSON");

        final Optional<WorkerFleetAdditionalFixedProperties> response =
            rrUtils.getWorkerFleetAdditionalFixedProperties(WORKER_FLEET_ARN);

        assertEquals(Optional.empty(), response);
    }

    @Test
    void given_emptyAdditionalInformationJson_when_getWorkerFleetAdditionalFixedProperties_then_returnsEmpty() {
        when(mockRrClient.getWorkerFleet(any(GetWorkerFleetRequest.class))).thenReturn(mockWorkerFleetResult);
        when(mockWorkerFleetResult.getAdditionalFixedProperties()).thenReturn(null);

        final Optional<WorkerFleetAdditionalFixedProperties> response =
            rrUtils.getWorkerFleetAdditionalFixedProperties(WORKER_FLEET_ARN);

        assertEquals(Optional.empty(), response);
    }

    @Test
    void given_emptySiteArn_when_createRobotIdToWorkernArnMap_then_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rrUtils.createRobotIdToWorkerArnMap("", WORKER_FLEET_ARN);
        });
    }

    @Test
    void given_emptyWorkerFleetArn_when_createRobotIdToWorkernArnMap_then_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rrUtils.createRobotIdToWorkerArnMap(SITE_ARN, "");
        });
    }

    @Test
    void given_workerExists_when_createRobotIdToWorkernArnMap_then_returnsValidMap() throws JsonProcessingException {
        final ListWorkersResult response = new ListWorkersResult()
                .withWorkers(List.of(worker1))
                .withNextToken(null);

        when(mockRrClient.listWorkers(any(ListWorkersRequest.class))).thenReturn(response);

        final VendorProperties vendorProperties = mock(VendorProperties.class);
        when(vendorProperties.getVendorWorkerId()).thenReturn("7");
        when(worker1.getVendorProperties()).thenReturn(vendorProperties);
        when(worker1.getArn()).thenReturn(WORKER_ARN);

        final Map<String, String> result =
            rrUtils.createRobotIdToWorkerArnMap(SITE_ARN, WORKER_FLEET_ARN);
        assertEquals(result.size(), 1);
        assertEquals(result.get("7"), WORKER_ARN);
    }

    

    @Test
    void given_listWorkersReturnsListOfWorkers_when_createRobotIdToWorkernArnMap_then_returnsListOfWorkers() {
        final int numWorkers = 3;
        final ListWorkersResult responseWithNextToken = new ListWorkersResult()
                .withWorkers(List.of(worker1, worker2))
                .withNextToken("nextToken");

        final ListWorkersResult responseWithoutNextToken = new ListWorkersResult()
                .withWorkers(List.of(worker3))
                .withNextToken(null);

        when(mockRrClient.listWorkers(any(ListWorkersRequest.class)))
                .thenReturn(responseWithNextToken)
                .thenReturn(responseWithoutNextToken);
        
        // Info for worker 1
        final VendorProperties vendorProperties1 = mock(VendorProperties.class);
        when(vendorProperties1.getVendorWorkerId()).thenReturn("7");
        when(worker1.getVendorProperties()).thenReturn(vendorProperties1);
        when(worker1.getArn()).thenReturn(WORKER_ARN);

        // Info for worker 2
        final VendorProperties vendorProperties2 = mock(VendorProperties.class);
        when(vendorProperties2.getVendorWorkerId()).thenReturn("8");
        when(worker2.getVendorProperties()).thenReturn(vendorProperties2);
        when(worker2.getArn()).thenReturn(WORKER_ARN);

        // Info for worker 3
        final VendorProperties vendorProperties3 = mock(VendorProperties.class);
        when(vendorProperties3.getVendorWorkerId()).thenReturn("9");
        when(worker3.getVendorProperties()).thenReturn(vendorProperties3);
        when(worker3.getArn()).thenReturn(WORKER_ARN);

        final Map<String, String> result =
            rrUtils.createRobotIdToWorkerArnMap(SITE_ARN, WORKER_FLEET_ARN);
        assertEquals(result.size(), numWorkers);
        assertTrue(result.containsKey("7"));
        assertEquals(WORKER_ARN, result.get("7"));
        assertTrue(result.containsKey("8"));
        assertEquals(WORKER_ARN, result.get("8"));
        assertTrue(result.containsKey("9"));
        assertEquals(WORKER_ARN, result.get("9"));
    }

    @Test
    void given_emptySiteArn_when_createWorkerArnToRobotIpMap_then_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rrUtils.createWorkerArnToRobotIpAddressMap("", WORKER_FLEET_ARN);
        });
    }

    @Test
    void given_emptyWorkerFleetArn_when_createWorkerArnToRobotIpMap_then_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rrUtils.createWorkerArnToRobotIpAddressMap(SITE_ARN, "");
        });
    }

    @Test
    void given_workerExists_when_createWorkerArnToRobotIpMap_then_returnsValidMap() throws JsonProcessingException {
        final ListWorkersResult response = new ListWorkersResult()
                .withWorkers(List.of(worker1))
                .withNextToken(null);

        when(mockRrClient.listWorkers(any(ListWorkersRequest.class))).thenReturn(response);

        final VendorProperties vendorProperties = mock(VendorProperties.class);
        when(vendorProperties.getVendorWorkerIpAddress()).thenReturn(ROBOT_IP);
        when(worker1.getVendorProperties()).thenReturn(vendorProperties);
        when(worker1.getArn()).thenReturn(WORKER_ARN);

        final Map<String, String> result =
            rrUtils.createWorkerArnToRobotIpAddressMap(SITE_ARN, WORKER_FLEET_ARN);
        assertEquals(1, result.size());
        assertEquals(ROBOT_IP, result.get(WORKER_ARN));
    }

    

    @Test
    void given_listWorkersReturnsListOfWorkers_when_createWorkerArnToRobotIpMap_then_returnsListOfWorkers() {
        final int numWorkers = 3;
        final ListWorkersResult responseWithNextToken = new ListWorkersResult()
                .withWorkers(List.of(worker1, worker2))
                .withNextToken("nextToken");

        final ListWorkersResult responseWithoutNextToken = new ListWorkersResult()
                .withWorkers(List.of(worker3))
                .withNextToken(null);

        when(mockRrClient.listWorkers(any(ListWorkersRequest.class)))
                .thenReturn(responseWithNextToken)
                .thenReturn(responseWithoutNextToken);


        final String worker1Arn = TestUtils.generateSimilarArn(WORKER_ARN);
        final String worker2Arn = TestUtils.generateSimilarArn(WORKER_ARN);
        final String worker3Arn = TestUtils.generateSimilarArn(WORKER_ARN);
        
        // Info for worker 1
        final VendorProperties vendorProperties1 = mock(VendorProperties.class);
        when(vendorProperties1.getVendorWorkerIpAddress()).thenReturn(ROBOT_IP);
        when(worker1.getVendorProperties()).thenReturn(vendorProperties1);
        when(worker1.getArn()).thenReturn(worker1Arn);

        // Info for worker 2
        final VendorProperties vendorProperties2 = mock(VendorProperties.class);
        when(vendorProperties2.getVendorWorkerIpAddress()).thenReturn(ROBOT_IP);
        when(worker2.getVendorProperties()).thenReturn(vendorProperties2);
        when(worker2.getArn()).thenReturn(worker2Arn);

        // Info for worker 3
        final VendorProperties vendorProperties3 = mock(VendorProperties.class);
        when(vendorProperties3.getVendorWorkerIpAddress()).thenReturn(ROBOT_IP);
        when(worker3.getVendorProperties()).thenReturn(vendorProperties3);
        when(worker3.getArn()).thenReturn(worker3Arn);

        final Map<String, String> result =
            rrUtils.createWorkerArnToRobotIpAddressMap(SITE_ARN, WORKER_FLEET_ARN);
        assertEquals(result.size(), numWorkers);
        assertTrue(result.containsKey(worker1Arn));
        assertEquals(ROBOT_IP, result.get(worker1Arn));
        assertTrue(result.containsKey(worker2Arn));
        assertEquals(ROBOT_IP, result.get(worker2Arn));
        assertTrue(result.containsKey(worker3Arn));
        assertEquals(ROBOT_IP, result.get(worker3Arn));
    }
}
