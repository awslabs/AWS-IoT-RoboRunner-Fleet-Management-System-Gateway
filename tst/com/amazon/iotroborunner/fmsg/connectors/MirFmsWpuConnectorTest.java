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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
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
import com.amazon.iotroborunner.fmsg.clients.MirFmsHttpClient;
import com.amazon.iotroborunner.fmsg.clients.SecretsManagerClientProvider;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.translations.MirFmsResponseTranslator;
import com.amazon.iotroborunner.fmsg.translations.OrientationTranslation;
import com.amazon.iotroborunner.fmsg.translations.PositionTranslation;
import com.amazon.iotroborunner.fmsg.types.FmsHttpRequest;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.roborunner.OrientationOffset;
import com.amazon.iotroborunner.fmsg.types.roborunner.PositionConversionCalibrationPoint;
import com.amazon.iotroborunner.fmsg.types.roborunner.ReferencePoint;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerFleetAdditionalFixedProperties;
import com.amazon.iotroborunner.fmsg.utils.RoboRunnerUtils;
import com.amazon.iotroborunner.fmsg.utils.SecretsManagerUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import lombok.extern.log4j.Log4j2;
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
 * Unit tests for the MiR FMS connector module.
 */
@Log4j2
@ExtendWith(MockitoExtension.class)
public class MirFmsWpuConnectorTest {
    private MirFmsConnector connector;

    @Mock
    private FmsgConnectorConfiguration mockFleetManagerConfig;

    @Mock
    private MirFmsHttpClient mockFmsClient;

    @Mock
    private AWSIoTRoboRunner mockRrClient;

    @Mock
    private AWSSecretsManager mockSecretsManagerClient;

    @Mock
    private ScheduledExecutorService mockExecutor;

    private final List<PositionConversionCalibrationPoint> mapPoints = Arrays.asList(
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

    private static Logger logger;
    private static MockedAppender mockedAppender;
    private WorkerStatus mockWorkerStatus;

    private void initMocks() {
        when(mockFleetManagerConfig.getFleetType()).thenReturn(RobotFleetType.MIR.value);
        when(mockFleetManagerConfig.getApiEndpoint()).thenReturn(TestConstants.VENDOR_API_ENDPOINT);
        when(mockFleetManagerConfig.getApiSecretName()).thenReturn(TestConstants.Secret.GENERAL_SECRET.name);
        when(mockFleetManagerConfig.getSiteArn()).thenReturn(TestConstants.SITE_ARN);
        when(mockFleetManagerConfig.getAwsRegion()).thenReturn(TestConstants.Region.EU_CENTRAL_1.name);
        when(mockFleetManagerConfig.getWorkerFleetArn()).thenReturn(TestConstants.WORKER_FLEET_ARN);
    }

    /**
     * Set up Worker Fleet resource with position conversion and orientation offset configurations.
     */
    private void setUpWorkerFleetAdditionalFixedProperties() {
        workerFleetProperties.setSchemaVersion("1.0");
        workerFleetProperties.setPositionConversion(mapPoints);
        workerFleetProperties.setOrientationOffset(orientationOffset);
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

    /**
     * Set up mocks required in the tests.
     */
    @BeforeEach
    public void setup() {
        initMocks();
        setUpWorkerFleetAdditionalFixedProperties();
        try (
            MockedConstruction<MirFmsHttpClient> clientMock = mockConstruction(MirFmsHttpClient.class,
                (mock, context) -> {
                    mockFmsClient = mock;
                    when(mock.sendFmsRequest(any(FmsHttpRequest.class))).thenReturn("test");
                }
            );
            MockedConstruction<PositionTranslation> posTransMock = mockConstruction(PositionTranslation.class);
            MockedConstruction<MirFmsResponseTranslator> responseTranslatorMock =
                mockConstruction(MirFmsResponseTranslator.class,
                    (mock, context) -> {
                        when(mock.getWorkerStatusFromFmsResponse(
                                anyString(),
                                anyString(),
                                any(PositionTranslation.class),
                                any(OrientationTranslation.class)))
                            .thenReturn(mockWorkerStatus);
                    }
                );
            MockedStatic<Executors> mockExecutors = mockStatic(Executors.class);
            MockedConstruction<RoboRunnerUtils> rrUtilsMock = mockConstruction(RoboRunnerUtils.class,
                (mock, context) -> {
                    when(mock.getWorkerFleetAdditionalFixedProperties(anyString()))
                        .thenReturn(Optional.of(workerFleetProperties));
                    when(mock.createRobotIdToWorkerArnMap(anyString(), anyString()))
                        .thenReturn(Map.of("test", "test"));
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
            connector.setupWorkerPropertyUpdates();
        }
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
    public void given_validParameters_when_constructed_then_passes() {
        assertNotNull(connector);
    }

    @Test
    public void given_validRobotId_when_getRobotStatusById_then_returnsWorkerStatus() {
        assertEquals(mockWorkerStatus, connector.getRobotStatusById("TheMuffinMan"));
    }

    @Test
    public void given_validRobots_when_getAllRobotStatuses_then_callsSendFmsRequest() throws Exception {
        // Immediately run the schedule task since the executor is mocked.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(mockExecutor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

        connector.getAllRobotStatuses();

        verify(mockFmsClient, times(1)).sendFmsRequest(any(FmsHttpRequest.class));
    }

    @Test
    public void given_getAllRobotStatusesCalled_when_getAllRobotStatusesCalled_then_scheduledAtFixedRateCalledOnce() {
        connector.getAllRobotStatuses();

        verify(this.mockExecutor, times(1)).scheduleAtFixedRate(any(Runnable.class),
            anyLong(), anyLong(), any(TimeUnit.class));

        connector.getAllRobotStatuses();

        verify(this.mockExecutor, times(1)).scheduleAtFixedRate(
            any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void given_scheduledGetRobotStatusRequests_when_stopGetAllRobotStatuses_then_futureCancelled() {
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(this.mockExecutor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
            .thenReturn(mockFuture);

        connector.getAllRobotStatuses();

        connector.stopGetAllRobotStatuses();

        verify(mockFuture, times(1)).cancel(anyBoolean());
    }

    @Test
    public void given_getRobotStatusesNotScheduled_when_stopGetAllRobotStatuses_then_futureNotCancelled() {
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);

        connector.stopGetAllRobotStatuses();

        verify(mockFuture, times(0)).cancel(anyBoolean());
    }
}
