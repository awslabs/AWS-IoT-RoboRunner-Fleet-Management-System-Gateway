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

package com.amazon.iotroborunner.fmsg.connectors;

import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants.POLYGON_JSON_1;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants.POLYGON_JSON_2;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants.POLYGON_JSON_3;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants.VENDOR_POLLING_DURATION;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createSharedSpaceTestResource;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createVendorSharedSpaceTestResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.clients.IotRoboRunnerJavaClientProvider;
import com.amazon.iotroborunner.fmsg.clients.MirFmsHttpClient;
import com.amazon.iotroborunner.fmsg.clients.SecretsManagerClientProvider;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.testhelpers.TestUtils;
import com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants;
import com.amazon.iotroborunner.fmsg.translations.MirFmsResponseTranslator;
import com.amazon.iotroborunner.fmsg.translations.PositionTranslation;
import com.amazon.iotroborunner.fmsg.types.FmsHttpRequest;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpacePosition;
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;
import com.amazon.iotroborunner.fmsg.utils.RoboRunnerUtils;
import com.amazon.iotroborunner.fmsg.utils.SecretsManagerUtils;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.CartesianCoordinates;
import com.amazonaws.services.iotroborunner.model.DestinationState;
import com.amazonaws.services.iotroborunner.model.Orientation;
import com.amazonaws.services.iotroborunner.model.PositionCoordinates;
import com.amazonaws.services.iotroborunner.model.VendorProperties;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the MiR FMS connector module. */
@ExtendWith(MockitoExtension.class)
public class MirFmsSharedSpaceConnectorTest {
    private MirFmsConnector connector;
    private String fmsAreaMapResponse;
    private String fmsWorkerResponse;
    private String fmsWorkerResponse2;
    private String fmsWaitingWorkerResponse;
    private WorkerStatus fmsWorkerStatusResponse;

    @Mock
    private FmsgConnectorConfiguration mockFleetManagerConfig;

    @Mock
    private MirFmsHttpClient mockFmsClient;

    @Mock
    private MirFmsHttpClient mockFmsSharedSpaceClient;

    @Mock
    private AWSIoTRoboRunner mockRrClient;

    @Mock
    private AWSSecretsManager mockSecretsManagerClient;

    @Mock
    private ScheduledExecutorService mockExecutor;

    @Mock
    private SharedSpaceClient sharedSpaceClient;
    
    private static Logger logger;
    private static MockedAppender mockedAppender;
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    private void initMocks() {
        when(mockFleetManagerConfig.getFleetType()).thenReturn(RobotFleetType.MIR.value);
        when(mockFleetManagerConfig.getApiEndpoint()).thenReturn(TestConstants.VENDOR_API_ENDPOINT);
        when(mockFleetManagerConfig.getApiSecretName()).thenReturn(TestConstants.Secret.GENERAL_SECRET.name);
        when(mockFleetManagerConfig.getSiteArn()).thenReturn(TestConstants.SITE_ARN);
        when(mockFleetManagerConfig.getAwsRegion()).thenReturn(TestConstants.Region.EU_CENTRAL_1.name);
        when(mockFleetManagerConfig.getWorkerFleetArn()).thenReturn(TestConstants.WORKER_FLEET_ARN);
    }

    /**
     * Set up logger required in the tests.
     */
    @BeforeAll
    public static void setUpLogger() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(MirFmsConnector.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
    }

    /** Set up mocks required in the tests. */
    @BeforeEach
    public void setup() throws IOException {
        fmsAreaMapResponse = Files.readString(
            Paths.get(SharedSpaceTestConstants.AREA_MAP_RESPONSE),
            StandardCharsets.US_ASCII);
        assertTrue(StringUtils.isNotBlank(fmsAreaMapResponse));

        fmsWorkerResponse = Files.readString(
            Paths.get(SharedSpaceTestConstants.WORKER_RESPONSE),
            StandardCharsets.US_ASCII);
        assertTrue(StringUtils.isNotBlank(fmsWorkerResponse));

        fmsWorkerResponse2 = Files.readString(
            Paths.get(SharedSpaceTestConstants.WORKER_RESPONSE_2),
            StandardCharsets.US_ASCII);
        assertTrue(StringUtils.isNotBlank(fmsWorkerResponse2));

        fmsWaitingWorkerResponse = Files.readString(
            Paths.get(SharedSpaceTestConstants.WORKER_WAITING_RESPONSE),
            StandardCharsets.US_ASCII);
        assertTrue(StringUtils.isNotBlank(fmsWaitingWorkerResponse));

        fmsWorkerStatusResponse = WorkerStatus.builder()
            .position(new PositionCoordinates()
                .withCartesianCoordinates(new CartesianCoordinates()
                    .withX(SharedSpaceTestConstants.ROBOT_POSITION_X)
                    .withY(SharedSpaceTestConstants.ROBOT_POSITION_Y)))
            .orientation(new Orientation())
            .vendorProperties(new VendorProperties())
            .workerAdditionalTransientProperties("").build();

        initMocks();

        try (

            MockedConstruction<MirFmsHttpClient> clientMock = mockConstruction(MirFmsHttpClient.class,
                (mock, context) -> {
                    mockFmsClient = mock;
                }
            );
            MockedConstruction<PositionTranslation> posTransMock = mockConstruction(PositionTranslation.class);
            MockedConstruction<MirFmsResponseTranslator> responseTranslatorMock =
                mockConstruction(MirFmsResponseTranslator.class,
                    (mock, context) -> {
                        when(mock.getWorkerStatusFromFmsResponse(anyString(), anyString(), eq(null), eq(null)))
                            .thenReturn(fmsWorkerStatusResponse);
                        when(mock.getWorkerStatusFromFmsResponse(anyString(), eq(""), eq(null), eq(null)))
                            .thenReturn(null);
                        when(mock.getWorkerStatusFromFmsResponse(anyString(), eq("emptyPosition"), eq(null), eq(null)))
                            .thenReturn(null);
                    }
                );
            MockedStatic<Executors> mockExecutors = mockStatic(Executors.class);
            MockedConstruction<RoboRunnerUtils> mockRrUtils = mockConstruction(RoboRunnerUtils.class,
                (mock, context) -> {
                    when(mock.getWorkerFleetAdditionalFixedProperties(anyString()))
                        .thenReturn(Optional.empty());
                    when(mock.createRobotIdToWorkerArnMap(anyString(), anyString()))
                        .thenReturn(Map.of("1", TestConstants.WORKER_ARN));
                });
            MockedConstruction<IotRoboRunnerJavaClientProvider> rrClientProviderMock =
                mockConstruction(IotRoboRunnerJavaClientProvider.class,
                    (mock, context) -> {
                        when(mock.getAwsIotRoboRunnerClient(anyString())).thenReturn(mockRrClient);
                    });
            MockedStatic<SecretsManagerUtils> mockSecretsManagerUtils = mockStatic(SecretsManagerUtils.class);
            MockedConstruction<SecretsManagerClientProvider> secretsManagerClientProviderMock =
                mockConstruction(SecretsManagerClientProvider.class,
                    (mock, context) -> {
                        when(mock.getAwsSecretsManagerClient(anyString())).thenReturn(mockSecretsManagerClient);
                    });
        ) {
            mockSecretsManagerUtils.when(
                    () -> SecretsManagerUtils.getSecret(any(AWSSecretsManager.class), anyString()))
                .thenReturn("test");
            mockExecutors.when(() -> Executors.newScheduledThreadPool(anyInt())).thenReturn(mockExecutor);
            connector = new MirFmsConnector(mockFleetManagerConfig);
        }
    }

    @Test
    void given_vendorSharedSpaceGuid_when_getSharedSpacePositions_then_returnPositionCoordinates() {
        when(mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn(fmsAreaMapResponse);
        final String guid = TestUtils.generateId();

        final Optional<SharedSpacePosition> result = connector.getSharedSpacePositions(guid);

        assertTrue(result.isPresent());
        final SharedSpacePosition sharedSpacePosition = result.get();
        assertEquals(sharedSpacePosition.getCoordinates().get(0).getX(), 49.15);
        assertEquals(sharedSpacePosition.getCoordinates().get(0).getY(), 74.3);
        assertEquals(sharedSpacePosition.getCoordinates().get(1).getX(), 64.9);
        assertEquals(sharedSpacePosition.getCoordinates().get(1).getY(), 74.2);
        assertEquals(sharedSpacePosition.getCoordinates().get(2).getY(), 58.3);
        assertEquals(sharedSpacePosition.getCoordinates().get(2).getX(), 62.35);
        assertEquals(sharedSpacePosition.getCoordinates().get(3).getX(), 77.7);
        assertEquals(sharedSpacePosition.getCoordinates().get(3).getY(), 59.3);
    }

    /**
     * Clean Up the used test resources.
     */
    @AfterAll
    public static void cleanUp() {
        if (mockedAppender != null) {
            logger.removeAppender(mockedAppender);
        }
    }

    @AfterEach
    public void clearLogHistory() {
        mockedAppender.clear();
    }

    @Test
    void given_emptyVendorPolygonJson_when_getSharedSpacePositions_then_returnEmptyList() {
        when(mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn(TestConstants.POLYGON_JSON_EMPTY);
        final String guid = TestUtils.generateId();

        final Optional<SharedSpacePosition> result = connector.getSharedSpacePositions(guid);

        assertTrue(result.isPresent());
        final SharedSpacePosition sharedSpacePosition = result.get();
        assertTrue(sharedSpacePosition.getCoordinates().isEmpty());
    }

    @Test
    void given_emptyFmsResponse_when_getSharedSpacePositions_then_returnEmptyOptional() {
        when(mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn("");
        final String guid = TestUtils.generateId();

        assertTrue(connector.getSharedSpacePositions(guid).isEmpty());
    }

    @Test
    void given_validRobotId_when_extractRobotVendorPositionPoint_then_returnRobotPosition() {
        when(mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn(fmsWorkerResponse);
        final String guid = TestUtils.generateId();

        final Optional<Point> result = connector.extractRobotVendorPositionPoint(guid);

        assertTrue(result.isPresent());
        assertEquals(result.get(), GEOMETRY_FACTORY.createPoint(new Coordinate(
            SharedSpaceTestConstants.ROBOT_VENDOR_X, SharedSpaceTestConstants.ROBOT_VENDOR_Y)));
    }

    @Test
    void given_emptyRobotId_when_extractRobotVendorPositionPoint_then_returnEmptyOptional() {
        final Optional<Point> result = connector.extractRobotVendorPositionPoint("");

        assertTrue(result.isEmpty());
    }

    @Test
    void given_emptyWorkerStatusPosition_when_extractRobotVendorPositionPoint_then_returnEmptyOptional() {
        final Optional<Point> result = connector.extractRobotVendorPositionPoint("emptyPosition");

        assertTrue(result.isEmpty());
    }

    @Test
    void given_waitingWorkerResponse_when_robotIsWaitingForSharedSpace_then_returnTrue() {
        when(mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn(fmsWaitingWorkerResponse);

        assertTrue(connector.robotIsWaitingForSharedSpace("test"));
    }

    @Test
    void given_notWaitingWorkerResponse_when_robotIsWaitingForSharedSpace_then_returnFalse() {
        when(mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn(fmsWorkerResponse);
        
        assertFalse(connector.robotIsWaitingForSharedSpace("test"));
    }

    @Test
    public void given_workerWeCanUnblock_when_grantWorkerAccessToSharedSpace_then_unblockAndBlockSharedSpace() {
        final VendorSharedSpace vendorSharedSpace = createVendorSharedSpaceTestResource(
            TestConstants.WORKER_FLEET_ARN, TestUtils.generateId());
        final SharedSpace sharedSpace = createSharedSpaceTestResource("SharedSpace", TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendorSharedSpace);
        final String successfulUnblockLog = String.format("Unblocked shared space: %s for worker: %s",
            TestConstants.DESTINATION_ARN, TestConstants.WORKER_ARN);
        try (
            MockedConstruction<SharedSpaceClient> sharedSpaceClientMock =
                mockConstruction(SharedSpaceClient.class,
                    (mock, context) -> {
                        sharedSpaceClient = mock;
                        when(this.mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class)))
                            .thenReturn(POLYGON_JSON_1);
                        when(this.sharedSpaceClient.getAllSharedSpaces(anyString(), anyString()))
                            .thenReturn(List.of(sharedSpace));
                    })
        ) {
            connector.setupSharedSpaceManagement();
        }

        connector.grantWorkerAccessToSharedSpace(TestConstants.WORKER_ARN, TestConstants.DESTINATION_ARN);

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(successfulUnblockLog)));
    }

    @Test
    public void given_failedUnblocking_when_grantWorkerAccessToSharedSpace_then_invokeFailureCallback() {
        final VendorSharedSpace vendorSharedSpace = createVendorSharedSpaceTestResource(
            TestConstants.WORKER_FLEET_ARN, TestUtils.generateId());
        final SharedSpace sharedSpace = createSharedSpaceTestResource("SharedSpace", TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendorSharedSpace);
        final String failureResponse = String.format("[FAILURE] Unable to unblock shared space: %s for worker: %s",
            TestConstants.DESTINATION_ARN, TestConstants.WORKER_ARN);
        try (
            MockedConstruction<SharedSpaceClient> sharedSpaceClientMock =
                mockConstruction(SharedSpaceClient.class,
                    (mock, context) -> {
                        sharedSpaceClient = mock;
                        when(this.mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class)))
                            .thenReturn(POLYGON_JSON_1).thenReturn(null);
                        when(this.sharedSpaceClient.getAllSharedSpaces(anyString(), anyString()))
                            .thenReturn(List.of(sharedSpace));
                    })
        ) {
            connector.setupSharedSpaceManagement();
        }

        connector.grantWorkerAccessToSharedSpace(TestConstants.WORKER_ARN, TestConstants.DESTINATION_ARN);

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(failureResponse)));
    }

    @Test
    public void given_alreadyRunningExecutor_when_listenToSharedSpaces_then_logAnError() {
        setupBasicSharedSpaceManagementMock();
        final String alreadyRunningLog = "Runnable already started for";

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);
        verify(this.mockExecutor, times(2)).scheduleAtFixedRate(any(Runnable.class),
            anyLong(), anyLong(), any(TimeUnit.class));

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);
        verify(this.mockExecutor, times(2)).scheduleAtFixedRate(
            any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith(alreadyRunningLog)));
    }

    @Test
    public void given_runningSharedSpaceListener_when_stopListeningToSharedSpaces_then_stopListening() {
        setupBasicSharedSpaceManagementMock();
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(this.mockExecutor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
            .thenReturn(mockFuture);

        connector.listenToSharedSpaces(VENDOR_POLLING_DURATION);
        connector.stopListeningToSharedSpaces();

        verify(mockFuture, times(2)).cancel(anyBoolean());
    }

    @Test
    public void given_workerStillInSharedSpace_when_monitorSharedSpaceExits_then_logStillPresent() {
        final VendorSharedSpace vendorSharedSpace = createVendorSharedSpaceTestResource(
            TestConstants.WORKER_FLEET_ARN, TestUtils.generateId());
        final SharedSpace sharedSpace = createSharedSpaceTestResource("SharedSpace", TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendorSharedSpace);
        final String stillWithinSharedSpaceLog = "Robot 1 still within the shared space";
        try (
            MockedConstruction<SharedSpaceClient> sharedSpaceClientMock =
                mockConstruction(SharedSpaceClient.class,
                    (mock, context) -> {
                        sharedSpaceClient = mock;
                        when(this.mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class)))
                            .thenReturn(POLYGON_JSON_1)
                            .thenReturn(fmsWorkerResponse);
                        when(this.sharedSpaceClient.getAllSharedSpaces(anyString(), anyString()))
                            .thenReturn(List.of(sharedSpace));
                    })
        ) {
            connector.setupSharedSpaceManagement();
        }
        connector.grantWorkerAccessToSharedSpace(TestConstants.WORKER_ARN, TestConstants.DESTINATION_ARN);

        connector.monitorSharedSpaceExits();

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(stillWithinSharedSpaceLog)));
    }

    @Test
    public void given_workerStillEntering_when_monitorSharedSpaceExits_then_logStillEntering() {
        final VendorSharedSpace vendorSharedSpace = createVendorSharedSpaceTestResource(
            TestConstants.WORKER_FLEET_ARN, TestUtils.generateId());
        final SharedSpace sharedSpace = createSharedSpaceTestResource("SharedSpace", TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendorSharedSpace);
        final String stillEnteringLog = "Robot 1 is still entering the shared space";
        try (
            MockedConstruction<SharedSpaceClient> sharedSpaceClientMock =
                mockConstruction(SharedSpaceClient.class,
                    (mock, context) -> {
                        sharedSpaceClient = mock;
                        when(this.mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class)))
                            .thenReturn(POLYGON_JSON_2)
                            .thenReturn(fmsWorkerResponse);
                        when(this.sharedSpaceClient.getAllSharedSpaces(anyString(), anyString()))
                            .thenReturn(List.of(sharedSpace));
                    })
        ) {
            connector.setupSharedSpaceManagement();
        }
        connector.grantWorkerAccessToSharedSpace(TestConstants.WORKER_ARN, TestConstants.DESTINATION_ARN);

        connector.monitorSharedSpaceExits();

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(stillEnteringLog)));
    }

    @Test
    public void given_workerThatExited_when_monitorSharedSpaceExits_then_triggerExitCallback() {
        final VendorSharedSpace vendorSharedSpace = createVendorSharedSpaceTestResource(
            TestConstants.WORKER_FLEET_ARN, TestUtils.generateId());
        final SharedSpace sharedSpace = createSharedSpaceTestResource("SharedSpace", TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendorSharedSpace);
        final String expectedLog = String.format("Notifying SM app that robot 1 has exited shared space %s",
            TestConstants.DESTINATION_ARN);
        try (
            MockedConstruction<SharedSpaceClient> sharedSpaceClientMock =
                mockConstruction(SharedSpaceClient.class,
                    (mock, context) -> {
                        sharedSpaceClient = mock;
                        when(this.mockFmsClient.sendFmsRequest(any(FmsHttpRequest.class)))
                            .thenReturn(POLYGON_JSON_3)
                            .thenReturn(fmsWorkerResponse)
                            .thenReturn(fmsWorkerResponse)
                            .thenReturn(fmsWorkerResponse)
                            .thenReturn(fmsWorkerResponse2);
                        when(this.sharedSpaceClient.getAllSharedSpaces(anyString(), anyString()))
                            .thenReturn(List.of(sharedSpace));
                    })
        ) {
            connector.setupSharedSpaceManagement();
        }
        connector.grantWorkerAccessToSharedSpace(TestConstants.WORKER_ARN, TestConstants.DESTINATION_ARN);

        connector.monitorSharedSpaceExits();

        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(expectedLog)));
    }

    /**
     * Sets up a basic Shared Space Management mock so that all the required resources are initialized.
     */
    private void setupBasicSharedSpaceManagementMock() {
        try (
            MockedConstruction<SharedSpaceClient> sharedSpaceClientMock =
                mockConstruction(SharedSpaceClient.class,
                    (mock, context) -> {
                        sharedSpaceClient = mock;
                    })
        ) {
            connector.setupSharedSpaceManagement();
        }
    }
}