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

package com.amazon.iotroborunner.fmsg.connectors.simulatedconnector;

import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.SHARED_SPACE_ARN;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.OUT_OF_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.WAITING_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.DESTINATION_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.SITE_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_FLEET_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants.VENDOR_POLLING_DURATION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.clients.IotRoboRunnerJavaClientProvider;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;
import com.amazon.iotroborunner.fmsg.utils.RoboRunnerUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerResult;
import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the Simulated FMS connector module.
 */
@ExtendWith(MockitoExtension.class)
public class SimulatedFmsConnectorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SimulatedFmsConnector connector;

    @Mock
    private FmsgConnectorConfiguration mockFleetManagerConfig;

    @Mock
    private RoboRunnerUtils rrUtilsMock;

    @Mock
    private AWSIoTRoboRunner mockRrClient;

    @Mock
    private ScheduledExecutorService mockExecutor;

    private static Logger logger;
    private static MockedAppender mockedAppender;

    private void initMocks() {
        when(mockFleetManagerConfig.getFleetType()).thenReturn(RobotFleetType.SIMULATED.value);
        when(mockFleetManagerConfig.getSiteArn()).thenReturn(SITE_ARN);
        when(mockFleetManagerConfig.getWorkerFleetArn()).thenReturn(WORKER_FLEET_ARN);
    }

    /**
     * Set up logger required in the tests.
     */
    @BeforeAll
    public static void setUpLogger() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(SimulatedFmsConnector.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
    }

    /**
     * Set up mocks required in the tests.
     */
    @BeforeEach
    public void setup() throws IOException {
        initMocks();
        setupConstructionMocks(new ArrayList<>());
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
    void given_workerWeCanUnblock_when_grantWorkerAccessToSharedSpace_then_logsSuccessfullyGrantedAccess()
            throws JsonProcessingException {
        final String expectedMsg = "Granted access to worker";
        when(rrUtilsMock.updateRoboRunnerWorkerAdditionalTransientProperties(
                anyString(), any(WorkerAdditionalTransientProperties.class))).thenReturn(new UpdateWorkerResult());

        connector.grantWorkerAccessToSharedSpace(WORKER_ARN, DESTINATION_ARN);

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedMsg)));
    }

    @Test
    void given_workerWithInvalidTransientProperties_when_grantWorkerAccessToSharedSpace_then_throwsJsonProcessingException()
            throws JsonProcessingException {
        final String expectedMsg = "Could not update the worker with status";
        when(rrUtilsMock.updateRoboRunnerWorkerAdditionalTransientProperties(
                anyString(), any(WorkerAdditionalTransientProperties.class))).thenThrow(JsonProcessingException.class);

        connector.grantWorkerAccessToSharedSpace(WORKER_ARN, DESTINATION_ARN);

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedMsg)));
    }

    @Test
    public void given_alreadyRunningExecutor_when_listenToSharedSpaces_then_logAnError() {
        final String alreadyRunningLog = "Runnable already started for";

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);
        verify(this.mockExecutor, times(1)).scheduleAtFixedRate(any(Runnable.class),
            anyLong(), anyLong(), any(TimeUnit.class));

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);
        verify(this.mockExecutor, times(1)).scheduleAtFixedRate(
            any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(alreadyRunningLog)));
    }

    @Test
    public void given_workerWaitingForSharedSpace_when_listenToSharedSpaces_then_logsSuccessRequestToLockSharedSpace()
            throws JsonProcessingException {
        setupConstructionMocks(List.of(generateWorker(WAITING_FOR_SHARED_SPACE.value, DESTINATION_ARN)));
        final String successMsg = "Successfully requested access for worker";

        // Setup to immediately run the schedule task since the executor is mocked.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(mockExecutor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);

        verify(rrUtilsMock, times(1)).getWorkersInWorkerFleet(anyString(), anyString());
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.contains(successMsg)));
    }

    @Test
    public void given_workerWithLockOutOfSharedSpace_when_listenToSharedSpaces_then_logsSuccessReleaseLockSharedSpace()
            throws JsonProcessingException, NoSuchFieldException, IllegalAccessException {

        final String successMsg = "Successfully requested release for worker";
        final Worker worker = generateWorker(OUT_OF_SHARED_SPACE.value, DESTINATION_ARN);

        setupConstructionMocks(List.of(worker));

        // Since the only way to hit the logic of releasing the lock is having the lock within
        // the workersWithLocks class field, we will use reflections to create a lock in that field
        // as if the connector already recorded it itself.
        final Field workersWithLocks = connector.getClass().getDeclaredField("workersWithLocks");
        workersWithLocks.setAccessible(true);
        workersWithLocks.set(connector, new HashMap<>() {{
                put(worker.getArn(), DESTINATION_ARN);
            }}
        );

        // Setup to immediately run the schedule task once since the executor is mocked.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(mockExecutor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);

        verify(rrUtilsMock, times(1)).getWorkersInWorkerFleet(anyString(), anyString());
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.contains(successMsg)));
    }

    @Test
    public void given_workerNotCloseToSharedSpaces_when_listenToSharedSpaces_then_noLocksAcquiredOrReleased()
            throws JsonProcessingException {

        final String successMsgForLocking = "Successfully requested release for worker";
        final String successMsgForReleasingLock = "Successfully requested release for worker";
        final Worker worker = generateWorker("", "");

        setupConstructionMocks(List.of(worker));

        // Setup to immediately run the schedule task once since the executor is mocked.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(mockExecutor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);

        verify(rrUtilsMock, times(1)).getWorkersInWorkerFleet(anyString(), anyString());
        assertTrue(mockedAppender.message.stream().noneMatch(msg -> msg.contains(successMsgForLocking)));
        assertTrue(mockedAppender.message.stream().noneMatch(msg -> msg.contains(successMsgForReleasingLock)));
    }

    @Test
    public void given_runningSharedSpaceListener_when_stopListeningToSharedSpaces_then_stopListening() {
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(this.mockExecutor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(mockFuture);

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);
        connector.stopListeningToSharedSpaces();

        verify(mockFuture, times(1)).cancel(anyBoolean());
    }

    @Test
    public void given_notRunningSharedSpaceListener_when_stopListeningToSharedSpaces_then_logsError() {
        final String expectedErrorMsg = "Can't stop listening to shared spaces because it was never started for";

        connector.stopListeningToSharedSpaces();

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedErrorMsg)));
    }

    @Test
    public void given_none_when_setupWorkerPropertyUpdates_then_throwsUnsupportedOperationException() {
        final String expectedMsg = "Simulated FMS Connector does not support Worker Property Updates";
        connector.setupWorkerPropertyUpdates();
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedMsg)));
    }

    @Test
    public void given_none_when_getRobotStatusById_then_throwsUnsupportedOperationException() {
        final String expectedMsg = "Simulated FMS Connector does not support Worker Property Updates";
        connector.getRobotStatusById("someVendorWorkerId");
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedMsg)));
    }

    @Test
    public void given_none_when_getAllRobotStatuses_then_throwsUnsupportedOperationException() {
        final String expectedMsg = "Simulated FMS Connector does not support Worker Property Updates";
        connector.getAllRobotStatuses();
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedMsg)));
    }

    @Test
    public void given_none_when_stopGetAllRobotStatuses_then_throwsUnsupportedOperationException() {
        final String expectedMsg = "Simulated FMS Connector does not support Worker Property Updates";
        connector.stopGetAllRobotStatuses();
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(expectedMsg)));
    }

    /**
     * Sets up the mock test resources and provides a way to provide various worker resources
     * for return values.
     *
     * @param listOfWorkers the list of one or more workers to use as a return for mocked methods
     */
    private void setupConstructionMocks(@NonNull final List<Worker> listOfWorkers) {
        try (
            MockedStatic<Executors> mockExecutors = mockStatic(Executors.class);
            MockedConstruction<RoboRunnerUtils> rrUtilsMock = mockConstruction(RoboRunnerUtils.class,
                    (mock, context) -> {
                        this.rrUtilsMock = mock;
                        when(mock.getWorkersInWorkerFleet(SITE_ARN, WORKER_FLEET_ARN))
                                .thenReturn(listOfWorkers);
                    });
            MockedConstruction<IotRoboRunnerJavaClientProvider> rrClientProviderMock =
                mockConstruction(IotRoboRunnerJavaClientProvider.class,
                    (mock, context) -> {
                        when(mock.getAwsIotRoboRunnerClient(anyString())).thenReturn(mockRrClient);
                    });
        ) {
            mockExecutors.when(() -> Executors.newScheduledThreadPool(anyInt())).thenReturn(mockExecutor);
            connector = new SimulatedFmsConnector(mockFleetManagerConfig);
            connector.setupSharedSpaceManagement();
        }
    }

    /**
     * Generates a Worker resource and adds properties specific to testing SimulatedFmsConnector if given.
     *
     * @param locationStatus the worker's location status indicating where it is relative to a shared space
     * @return the constructed worker for testing
     * @throws JsonProcessingException if the custom transient properties cannot be mapped to string type
     */
    private Worker generateWorker(@NonNull final String locationStatus, @NonNull final String destinationArn)
            throws JsonProcessingException {
        final WorkerAdditionalTransientProperties addProps = new WorkerAdditionalTransientProperties();
        final Map<String, String> workerCustomTransientProperties = new HashMap<>() {{
                put(WORKER_LOCATION_STATUS.value, locationStatus);
                put(SHARED_SPACE_ARN.value, destinationArn);
            }};
        addProps.setCustomTransientProperties(workerCustomTransientProperties);

        return new Worker()
                .withSite(SITE_ARN)
                .withFleet(WORKER_FLEET_ARN)
                .withArn(WORKER_ARN)
                .withAdditionalTransientProperties(OBJECT_MAPPER.writeValueAsString(addProps));
    }
}
