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

package com.amazon.iotroborunner.fmsg.sharedspacemgmt;

import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.SITE_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.SHARED_SPACE_ARN_SINGLETON;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.SHARED_SPACE_DESTINATION_ADDITIONAL_INFO;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.SHARED_SPACE_DESTINATION_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.PriorityQueueTestConstants.PRIORITY_QUEUE_RECORD_WITHOUT_LOCK;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.PriorityQueueTestConstants.PRIORITY_QUEUE_RECORD_WITH_LOCK;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.PriorityQueueTestConstants.TIMED_OUT_PRIORITY_QUEUE_WITH_LOCK;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createDestinationTestResource;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_LOCK_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.PriorityQueueUtilsTest.requiredAttributes;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.PriorityQueueUtilsTest.requiredKses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;
import com.amazon.iotroborunner.fmsg.dynamodb.SharedSpaceManagementPriorityQueue;
import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.DestinationState;
import com.amazonaws.services.iotroborunner.model.ListDestinationsRequest;
import com.amazonaws.services.iotroborunner.model.ListDestinationsResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit Tests for the Shared Space Management (Mgmt) Application.
 */
@ExtendWith(MockitoExtension.class)
public class FmsgSharedSpaceMgmtTest {
    @Mock
    private FmsConnector connector;
    @Mock
    private Map<String, FmsConnector> connectorsByWorkerFleet;
    @Mock
    private AWSIoTRoboRunner roboRunnerClient;
    @Mock
    private ScheduledExecutorService executorService;
    @Mock
    private SharedSpaceManagementPriorityQueue priorityQueue;
    @Mock
    private AmazonDynamoDB dynamoDbClient;

    private FmsgSharedSpaceMgmt classUnderTest;

    private static Logger logger;
    private static MockedAppender mockedAppender;
    private static FmsgCoreConfiguration configs;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify that the log doesn't contain the provided message.
     *
     * @param message Expected message.
     */
    private void assertLogDoesntContainMessage(final String message) {
        assertFalse(mockedAppender.message.stream().anyMatch(msg -> msg.equals(message)));
    }

    /**
     * Set up the mocks needed for each subsequent test.
     */
    @BeforeAll
    public static void setUp() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(FmsgSharedSpaceMgmt.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
        configs = FmsgCoreConfiguration.builder()
            .siteArn(SITE_ARN)
            .spaceManagementEnabled(true)
            .maximumSharedSpaceCrossingTime(300)
            .vendorSharedSpacePollingInterval(3)
            .build();
    }

    @BeforeEach
    public void initializeTestClass() {
        classUnderTest = new FmsgSharedSpaceMgmt(configs, this.executorService, roboRunnerClient, dynamoDbClient,
            priorityQueue, connectorsByWorkerFleet);
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
    public void given_connectorMap_when_startSharedSpaceMgmt_then_startPolling() throws JsonProcessingException {
        final Destination destination = createDestinationTestResource("TestSharedSpaceDestination",
            SITE_ARN,
            SHARED_SPACE_DESTINATION_ARN,
            DestinationState.ENABLED,
            OBJECT_MAPPER.writeValueAsString(SHARED_SPACE_DESTINATION_ADDITIONAL_INFO));
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE);
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(this.dynamoDbClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);
        when(this.connectorsByWorkerFleet.size()).thenReturn(1);
        when(this.roboRunnerClient.listDestinations(any(ListDestinationsRequest.class))).thenReturn(
            new ListDestinationsResult().withNextToken(null).withDestinations(List.of(destination)));

        this.classUnderTest.startSharedSpaceMgmt(this.connectorsByWorkerFleet);

        mockedAppender.assertLogContainsMessage(String.format("Starting Shared Space Management with %s connectors",
            1));
        verify(this.executorService, times(1)).scheduleAtFixedRate(
            any(Runnable.class),
            anyLong(),
            anyLong(),
            any(TimeUnit.class));
    }

    @Test
    public void given_requestStopSharedSpaceMgmt_when_stopSharedSpaceMgmt_then_stopPolling() {
        this.classUnderTest.stopSharedSpaceMgmt();

        mockedAppender.assertLogContainsMessage("Shutting down Shared Space Management");
        mockedAppender.assertLogContainsMessage("Shut down Shared Space Management");
        verify(this.executorService, times(1)).shutdown();
    }

    @Test
    public void given_blockedAndTimedOutSharedSpace_when_manageSharedSpaces_then_logTimeout() {
        final PriorityQueueRecord record = TIMED_OUT_PRIORITY_QUEUE_WITH_LOCK;
        final String timeoutLog = String.format(
            "[TIMEOUT] Worker: %s failed to cross shared space: %s by required time: %s",
            record.getWorkerArn(),
            record.getSharedSpaceArn(),
            record.getMaxCrossingTime());
        when(this.priorityQueue.getCurrentLockHolder(SHARED_SPACE_ARN_SINGLETON.get(0)))
            .thenReturn(Optional.of(record));

        this.classUnderTest.manageSharedSpaces(SHARED_SPACE_ARN_SINGLETON, connectorsByWorkerFleet);

        mockedAppender.assertLogContainsMessage(timeoutLog);
    }

    @Test
    public void given_blockedSharedSpace_when_manageSharedSpaces_then_verifyNoTimeout() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITH_LOCK;
        final String timeoutLog = String.format(
            "[TIMEOUT] Worker: %s failed to cross shared space: %s by required time: %s",
            record.getWorkerArn(),
            record.getSharedSpaceArn(),
            record.getMaxCrossingTime());
        when(this.priorityQueue.getCurrentLockHolder(SHARED_SPACE_ARN_SINGLETON.get(0)))
            .thenReturn(Optional.of(record));

        this.classUnderTest.manageSharedSpaces(SHARED_SPACE_ARN_SINGLETON, connectorsByWorkerFleet);

        assertLogDoesntContainMessage(timeoutLog);
    }

    @Test
    public void given_unblockedSharedSpaceWithWaitingWorker_when_manageSharedSpace_then_grantAccessAndUpdateQueue() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITHOUT_LOCK;
        final String expectedLog = String.format(
            "Granted worker: %s access to shared space: %s",
            record.getWorkerArn(),
            record.getSharedSpaceArn());
        when(this.priorityQueue.getNextWorkerInQueue(SHARED_SPACE_ARN_SINGLETON.get(0)))
            .thenReturn(Optional.of(record));
        when(this.connectorsByWorkerFleet.containsKey(record.getWorkerFleet())).thenReturn(true);
        when(this.connectorsByWorkerFleet.get(record.getWorkerFleet())).thenReturn(connector);

        this.classUnderTest.manageSharedSpaces(SHARED_SPACE_ARN_SINGLETON, connectorsByWorkerFleet);

        mockedAppender.assertLogContainsMessage(expectedLog);
    }

    @Test
    public void given_unblockedSharedSpaceWithoutMatchingConnector_when_manageSharedSpace_then_throwDuringGrantAccess() {
        final PriorityQueueRecord record = PRIORITY_QUEUE_RECORD_WITHOUT_LOCK;
        final String expectedLog = String.format("Unable to grant worker: %s access to the shared space because there "
            + "is no connector for fleet: %s", record.getWorkerArn(), record.getWorkerFleet());

        when(this.priorityQueue.getNextWorkerInQueue(SHARED_SPACE_ARN_SINGLETON.get(0)))
            .thenReturn(Optional.of(record));
        when(this.connectorsByWorkerFleet.containsKey(record.getWorkerFleet())).thenReturn(false);

        this.classUnderTest.manageSharedSpaces(SHARED_SPACE_ARN_SINGLETON, connectorsByWorkerFleet);

        mockedAppender.assertLogContainsMessage(expectedLog);
    }

    @Test
    public void given_unblockedSharedSpaceWithoutWaitingWorker_when_manageSharedSpace_then_doNothing() {
        when(this.priorityQueue.getNextWorkerInQueue(SHARED_SPACE_ARN_SINGLETON.get(0))).thenReturn(Optional.empty());

        this.classUnderTest.manageSharedSpaces(SHARED_SPACE_ARN_SINGLETON, connectorsByWorkerFleet);

        assertFalse(mockedAppender.message.stream().anyMatch(msg -> msg.startsWith("Granted worker: ")));
    }

    @Test
    public void given_callbacks_when_startSharedSpaceMgmt_then_registerCallbacks() {
        final TableDescription tableDescription = new TableDescription()
            .withKeySchema(requiredKses)
            .withAttributeDefinitions(requiredAttributes)
            .withTableStatus(TableStatus.ACTIVE);
        final DescribeTableResult tableResult = new DescribeTableResult().withTable(tableDescription);
        when(this.connectorsByWorkerFleet.values()).thenReturn(List.of(this.connector));
        when(this.roboRunnerClient.listDestinations(any(ListDestinationsRequest.class))).thenReturn(
            new ListDestinationsResult().withNextToken(null).withDestinations(Collections.emptyList()));
        when(this.dynamoDbClient.describeTable(any(DescribeTableRequest.class))).thenReturn(tableResult);

        this.classUnderTest.startSharedSpaceMgmt(this.connectorsByWorkerFleet);

        verify(this.connector, times(1)).registerCallback(
            REQUEST_LOCK_FOR_SHARED_SPACE,
            this.classUnderTest.requestSharedSpaceCallback);
        verify(this.connector, times(1)).registerCallback(
            REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE,
            this.classUnderTest.releaseSharedSpaceCallback);
        verify(this.connector, times(1)).registerCallback(
            FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE,
            this.classUnderTest.failedAccessSharedSpaceCallback);
    }

    @Test
    public void given_callbacks_when_stopSharedSpaceMgmt_then_unregisterCallbacks() {
        when(this.connectorsByWorkerFleet.values()).thenReturn(List.of(this.connector));

        this.classUnderTest.stopSharedSpaceMgmt();

        verify(this.connector, times(1)).unregisterCallback(
            REQUEST_LOCK_FOR_SHARED_SPACE,
            this.classUnderTest.requestSharedSpaceCallback);
        verify(this.connector, times(1)).unregisterCallback(
            REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE,
            this.classUnderTest.releaseSharedSpaceCallback);
        verify(this.connector, times(1)).unregisterCallback(
            FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE,
            this.classUnderTest.failedAccessSharedSpaceCallback);
    }
}
