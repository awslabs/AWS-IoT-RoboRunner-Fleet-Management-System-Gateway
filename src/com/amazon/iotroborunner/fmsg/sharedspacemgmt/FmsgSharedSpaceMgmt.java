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

import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.LOCK_PRIORITY;
import static com.amazon.iotroborunner.fmsg.constants.PriorityQueueConstants.PRIORITY_QUEUE_TABLE_NAME;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_LOCK_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.convertToAccessSharedSpaceRequest;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.convertToFailureMessage;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.convertToReleaseSharedSpaceRequest;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.verifyWorkerHoldsLockForSharedSpace;

import com.amazon.iotroborunner.fmsg.clients.AmazonDynamoDbClientProvider;
import com.amazon.iotroborunner.fmsg.clients.IotRoboRunnerJavaClientProvider;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;
import com.amazon.iotroborunner.fmsg.dynamodb.SharedSpaceManagementPriorityQueue;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandCallback;
import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.PriorityQueueUtils;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.DestinationState;
import com.amazonaws.services.iotroborunner.model.ListDestinationsRequest;
import com.amazonaws.services.iotroborunner.model.UpdateDestinationRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Singleton class containing the logic for the SM Application.
 */
@Log4j2
@AllArgsConstructor
@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class FmsgSharedSpaceMgmt {
    private final String siteArn;
    private final Duration vendorPollingDuration;
    private final Duration maxCrossingTimeBuffer;
    private final AWSIoTRoboRunner roboRunnerClient;
    private final ScheduledExecutorService executorService;
    private final AmazonDynamoDB dynamoDbClient;
    private final SharedSpaceManagementPriorityQueue priorityQueue;
    private Map<String, FmsConnector> connectorsByWorkerFleet;

    private static final int ZERO_SECOND_DELAY = 0;
    private static final Duration ONE_DAY_TIME_BUFFER = Duration.ofDays(1);

    /**
     * Default Constructor.
     *
     * @param config FMSG communication
     */
    public FmsgSharedSpaceMgmt(@NonNull final FmsgCoreConfiguration config) {
        this.siteArn = config.getSiteArn();
        this.vendorPollingDuration = Duration.ofSeconds(config.getVendorSharedSpacePollingInterval());
        this.maxCrossingTimeBuffer = Duration.ofSeconds(config.getMaximumSharedSpaceCrossingTime());
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.roboRunnerClient = new IotRoboRunnerJavaClientProvider()
            .getAwsIotRoboRunnerClient(config.getAwsRegion());
        this.dynamoDbClient = new AmazonDynamoDbClientProvider().getAmazonDynamoDbClient(config.getAwsRegion());
        this.priorityQueue = new SharedSpaceManagementPriorityQueue(
            PRIORITY_QUEUE_TABLE_NAME,
            new DynamoDBMapper(this.dynamoDbClient)
        );
    }

    /**
     * All args constructor. (Visible for testing.)
     *
     * @param config                  RoboRunner Configurations
     * @param executorService         Scheduled Executor Service
     * @param roboRunnerClient        IoT RoboRunner Client
     * @param dynamoDbClient          DynamoDB Client
     * @param priorityQueue           Shared Space Management Priority Queue
     * @param connectorsByWorkerFleet connectors
     */
    public FmsgSharedSpaceMgmt(@NonNull final FmsgCoreConfiguration config,
                               @NonNull final ScheduledExecutorService executorService,
                               @NonNull final AWSIoTRoboRunner roboRunnerClient,
                               @NonNull final AmazonDynamoDB dynamoDbClient,
                               @NonNull final SharedSpaceManagementPriorityQueue priorityQueue,
                               @NonNull final Map<String, FmsConnector> connectorsByWorkerFleet) {
        this.siteArn = config.getSiteArn();
        this.vendorPollingDuration = Duration.ofSeconds(config.getVendorSharedSpacePollingInterval());
        this.maxCrossingTimeBuffer = Duration.ofSeconds(config.getMaximumSharedSpaceCrossingTime());
        this.executorService = executorService;
        this.roboRunnerClient = roboRunnerClient;
        this.priorityQueue = priorityQueue;
        this.connectorsByWorkerFleet = connectorsByWorkerFleet;
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Starts Shared Space Management execution which coordinates entrance and exits for all RoboRunner Shared Spaces.
     */
    public void startSharedSpaceMgmt(@NonNull final Map<String, FmsConnector> connectorsByWorkerFleet) {
        log.info("Verifying that {} table has been set up.", PRIORITY_QUEUE_TABLE_NAME);
        PriorityQueueUtils.createPriorityQueueIfMissing(this.dynamoDbClient, new DynamoDBMapper(this.dynamoDbClient));

        log.info("Starting Shared Space Management with {} connectors", connectorsByWorkerFleet.size());

        this.connectorsByWorkerFleet = connectorsByWorkerFleet;
        for (final FmsConnector connector : this.connectorsByWorkerFleet.values()) {
            activateConnectorListener(connector);
            registerSharedSpaceCallbacks(connector);
        }
        final List<String> roboRunnerSharedSpaces = locateRoboRunnerSharedSpaces(siteArn);
        executorService.scheduleAtFixedRate(
            () -> {
                manageSharedSpaces(roboRunnerSharedSpaces, connectorsByWorkerFleet);
            },
            ZERO_SECOND_DELAY,
            vendorPollingDuration.toSeconds(),
            TimeUnit.SECONDS);
    }

    /**
     * Shutdowns Shared Space Management execution.
     */
    public void stopSharedSpaceMgmt() {
        log.info("Shutting down Shared Space Management");
        unregisterSharedSpaceCallbacks();
        deactivateSharedSpaceListeners();
        executorService.shutdown();
        log.info("Shut down Shared Space Management");
    }

    /**
     * Manages the active shared spaces by determining what worker should enter the Shared Space and then
     * grants them permission. Also regulates to determine if a timeout crossing has happened.
     *
     * @param connectorsByWorkerFleet connectors that manage vendor FMS communication
     */
    public void manageSharedSpaces(@NonNull final List<String> sharedSpaceArns,
                                   @NonNull final Map<String, FmsConnector> connectorsByWorkerFleet) {

        for (final String sharedSpaceArn : sharedSpaceArns) {
            final Optional<PriorityQueueRecord> lockHolder = this.priorityQueue.getCurrentLockHolder(sharedSpaceArn);
            lockHolder.ifPresentOrElse(
                this::logCrossingTimeoutIfFound,
                () -> {
                    grantNextWorkerAccessToSharedSpace(sharedSpaceArn, connectorsByWorkerFleet);
                });
        }
    }

    /**
     * Callback function that requests a Shared Space for a worker.
     */
    public FmsCommandCallback requestSharedSpaceCallback = new FmsCommandCallback() {
        @Override
        public void onResponse(@NonNull final String response) {
            convertToAccessSharedSpaceRequest(response).ifPresent(
                request -> {
                    final PriorityQueueRecord record = PriorityQueueRecord.builder()
                        .workerArn(request.getWorkerArn())
                        .sharedSpaceArn(request.getSharedSpaceArn())
                        .workerFleet(request.getWorkerFleetArn())
                        .priority(String.valueOf(request.getRequestTime()))
                        .ttl(System.currentTimeMillis() + ONE_DAY_TIME_BUFFER.toMillis())
                        .build();
                    addLockRequestToPriorityQueue(record);
                }
            );
        }
    };

    /**
     * Callback function that releases a Shared Space for a worker.
     */
    public FmsCommandCallback releaseSharedSpaceCallback = new FmsCommandCallback() {
        @Override
        public void onResponse(@NonNull final String response) {
            convertToReleaseSharedSpaceRequest(response).ifPresent(
                request -> {
                    final Optional<PriorityQueueRecord> optionalLockHolder = getLockHolderRecord(
                        request.getSharedSpaceArn());

                    verifyWorkerHoldsLockForSharedSpace(request.getWorkerArn(), request.getSharedSpaceArn(),
                        optionalLockHolder)
                        .ifPresent(record -> {
                                removeLockFromWorkerInPriorityQueue(record);
                            }
                        );
                }
            );
        }
    };

    /**
     * Callback function that reports connection failure to Shared Space.
     */
    public FmsCommandCallback failedAccessSharedSpaceCallback = new FmsCommandCallback() {
        @Override
        public void onResponse(@NonNull final String response) {
            convertToFailureMessage(response).ifPresent(
                request -> {
                    log.error("[FAILURE] Unable to grant worker access to shared space because {}. Worker: {}, "
                            + "Shared Space: {}",
                        request.getMessage(),
                        request.getWorkerArn(),
                        request.getSharedSpaceArn()
                    );
                }
            );
        }
    };

    /**
     * Finds all the RoboRunner Shared Spaces for the given Site. A RoboRunner Shared Space is defined
     * as a Destination resource that has vendor Shared Space data stored in the additional-fixed-properties field.
     *
     * @param siteArn arn of the Site we're gathering shared spaces for
     * @return list of active RoboRunner Shared Space ARNs
     */
    private List<String> locateRoboRunnerSharedSpaces(@NonNull final String siteArn) {
        log.info("Fetching RoboRunner Shared Spaces for siteArn: {}", siteArn);
        final ListDestinationsRequest request = new ListDestinationsRequest().withSite(siteArn);
        // Decommissioned means the customer has taken an action to prevent RoboRunner from monitoring this shared space
        final List<Destination> activeSiteDestinations =
            this.roboRunnerClient.listDestinations(request).getDestinations().stream()
                .filter(destination -> !DestinationState.DECOMMISSIONED.toString().equals(destination.getState()))
                .toList();
        final List<String> sharedSpaceArns = SharedSpaceUtils.getRoboRunnerSharedSpaceArns(activeSiteDestinations);
        log.info("Found {} RoboRunner Shared Spaces: {}", sharedSpaceArns.size(), sharedSpaceArns);
        return sharedSpaceArns;
    }

    /**
     * Logs an info message if a connector reports that a time has occurred for the given worker. A timeout is
     * considered to have occurred if a worker hasn't exited the shared space prior to the maximum crossing time.
     *
     * @param workerRecord priorityQueueRecord holding details about the worker we're checking
     */
    private void logCrossingTimeoutIfFound(@NonNull final PriorityQueueRecord workerRecord) {
        log.debug("Checking to see if worker: {} experienced a timeout at shared space: {}",
            workerRecord.getWorkerArn(),
            workerRecord.getSharedSpaceArn());

        final Instant currentTime = Instant.now();
        final boolean hasTimedOut = currentTime.isAfter(Instant.ofEpochMilli(workerRecord.getMaxCrossingTime()));

        if (hasTimedOut) {
            log.warn("[TIMEOUT] Worker: {} failed to cross shared space: {} by required time: {}",
                workerRecord.getWorkerArn(), workerRecord.getSharedSpaceArn(), workerRecord.getMaxCrossingTime());
        }
    }

    /**
     * Grants the next approved worker access to the shared space. A worker is considered approved to enter a shared
     * space if it's the first worker in the queue for that shared space. Access is granted by commissioning the
     * appropriate FMS Connector to allow access via the vendor fleet management system.
     *
     * @param sharedSpaceArn          shared Space we need to find the next worker to enter
     * @param connectorsByWorkerFleet connectors by their respective worker fleets
     * @return optional PriorityQueueRecord of the worker who should be granted access
     */
    private Optional<PriorityQueueRecord> grantNextWorkerAccessToSharedSpace(
        @NonNull final String sharedSpaceArn,
        @NonNull final Map<String, FmsConnector> connectorsByWorkerFleet) {

        final Optional<PriorityQueueRecord> nextWorkerInQueue = this.priorityQueue.getNextWorkerInQueue(sharedSpaceArn);

        nextWorkerInQueue.ifPresent(worker -> {
            if (!connectorsByWorkerFleet.containsKey(worker.getWorkerFleet())) {
                log.error("Unable to grant worker: {} access to the shared space because there is no connector"
                    + " for fleet: {}", worker.getWorkerArn(), worker.getWorkerFleet());
                return;
            }

            final FmsConnector connector = connectorsByWorkerFleet.get(worker.getWorkerFleet());
            addLockToWorkerInPriorityQueue(worker);
            connector.grantWorkerAccessToSharedSpace(worker.getWorkerArn(), worker.getSharedSpaceArn());
            log.info("Granted worker: {} access to shared space: {}",
                worker.getWorkerArn(),
                worker.getSharedSpaceArn());
        });

        return nextWorkerInQueue;
    }

    /**
     * Updates the worker record to include LOCK as the priority and updates relevant timestamps to reflect the new
     * status of the worker.
     *
     * @param record record for the worker that should be given a lock
     */
    private void addLockToWorkerInPriorityQueue(@NonNull final PriorityQueueRecord record) {
        log.info("Updating priority queue so worker: {} has a LOCK for shared space: {}",
            record.getWorkerArn(),
            record.getSharedSpaceArn());

        final long currentTimestamp = Instant.now().toEpochMilli();
        final PriorityQueueRecord lockHoldingRecord = PriorityQueueRecord.builder()
            .sharedSpaceArn(record.getSharedSpaceArn())
            .workerArn(record.getWorkerArn())
            .workerFleet(record.getWorkerFleet())
            .priority(LOCK_PRIORITY)
            .maxCrossingTime(currentTimestamp + maxCrossingTimeBuffer.toMillis())
            .ttl(currentTimestamp + ONE_DAY_TIME_BUFFER.toMillis())
            .build();

        this.priorityQueue.transactionWrite(lockHoldingRecord, record);

        // Disabled means the shared space is occupied and can't accommodate another robot
        final UpdateDestinationRequest disableSharedSpaceRequest = new UpdateDestinationRequest()
            .withId(record.getSharedSpaceArn())
            .withState(DestinationState.DISABLED);
        this.roboRunnerClient.updateDestination(disableSharedSpaceRequest);

        log.debug("Updated worker: {} with the following record: {}",
            lockHoldingRecord.getWorkerArn(),
            lockHoldingRecord.toString());
    }

    /**
     * Releases lock on Shared Space for the requested worker.
     *
     * @param record record of the worker that should have its lock released
     */
    private void removeLockFromWorkerInPriorityQueue(@NonNull final PriorityQueueRecord record) {
        log.info("Releasing lock for worker: {} on shared space: {}",
            record.getWorkerArn(),
            record.getSharedSpaceArn());

        this.priorityQueue.deleteRecord(record);

        // Enabled means the shared space is not currently occupied but can be
        final UpdateDestinationRequest enableSharedSpaceRequest = new UpdateDestinationRequest()
            .withId(record.getSharedSpaceArn())
            .withState(DestinationState.ENABLED);
        this.roboRunnerClient.updateDestination(enableSharedSpaceRequest);

        log.debug("Removed the following record: {} from the priority queue.", record.toString());
    }

    /**
     * Add the provided PriorityQueueRecord to the Shared Space Management priority queue. Doing indicates that a
     * worker is requesting a lock for the shared space which means they would like to access the shared space.
     *
     * @param record record to add to the priority queue
     */
    private void addLockRequestToPriorityQueue(final PriorityQueueRecord record) {
        this.priorityQueue.addRecordRequestIfNotAlreadyPresent(record);
    }

    /**
     * Gets the PriorityQueueRecord that corresponds to the worker who currently has a lock for the provided shared
     * space. Being a lock holder means that the worker is the only resource approved to enter/cross the shared space.
     * If there is no worker with a lock for the shared space an empty Optional is returned.
     *
     * @param sharedSpaceArn shared space to query
     * @return optional of the lock holding record
     */
    private Optional<PriorityQueueRecord> getLockHolderRecord(@NonNull final String sharedSpaceArn) {
        return this.priorityQueue.getCurrentLockHolder(sharedSpaceArn);
    }

    /**
     * Register required Shared Space Management callbacks so that the connectors can communicate with the Shared Space
     * Management application.
     */
    private void registerSharedSpaceCallbacks(@NonNull final FmsConnector connector) {
        connector.registerCallback(REQUEST_LOCK_FOR_SHARED_SPACE, requestSharedSpaceCallback);
        connector.registerCallback(REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE, releaseSharedSpaceCallback);
        connector.registerCallback(FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE, failedAccessSharedSpaceCallback);
    }

    /**
     * Unregister required Shared Space Management callbacks in preparation of the application being shutdown. By
     * unregistering the callbacks, the connectors will no longer be able to communicate with the Shared Space
     * Management application.
     */
    private void unregisterSharedSpaceCallbacks() {
        if (this.connectorsByWorkerFleet == null) {
            final String msg = "Unable to unregister Shared Space Management (SM) callbacks because connectors "
                + "are uninitialized. These callbacks are required for the SM application to shutdown.";
            log.fatal(msg);
            throw new RuntimeException(msg);
        }
        for (final FmsConnector connector : this.connectorsByWorkerFleet.values()) {
            connector.unregisterCallback(REQUEST_LOCK_FOR_SHARED_SPACE, requestSharedSpaceCallback);
            connector.unregisterCallback(REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE, releaseSharedSpaceCallback);
            connector.unregisterCallback(FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE, failedAccessSharedSpaceCallback);
        }
    }

    /**
     * Turns ON the Shared Space listener for the provided connector. The listener allows the connector to determine
     * if there are workers waiting to enter a vendor shared space and determine when workers have exited vendor
     * shared spaces.
     *
     * @param connector connector to initialize resources for
     */
    private void activateConnectorListener(@NonNull final FmsConnector connector) {
        connector.listenToSharedSpaces(this.vendorPollingDuration);
    }

    /**
     * Turns OFF the Shared Space listener for the provided connectors. The listener allows the connector to determine
     * if there are workers waiting to enter a vendor shared space and determine when workers have exited vendor
     * shared spaces.
     */
    private void deactivateSharedSpaceListeners() {
        for (final FmsConnector connector : this.connectorsByWorkerFleet.values()) {
            connector.stopListeningToSharedSpaces();
        }
    }
}
