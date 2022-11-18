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

import static com.amazon.iotroborunner.fmsg.constants.RoboRunnerWorkerStatusConstants.JSON_SCHEMA_VERSION;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.SHARED_SPACE_ARN;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.IN_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_LOCK_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE;

import com.amazon.iotroborunner.fmsg.clients.IotRoboRunnerJavaClientProvider;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;
import com.amazon.iotroborunner.fmsg.constants.FmsgApplications;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.callback.AccessSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandCallback;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType;
import com.amazon.iotroborunner.fmsg.types.callback.ReleaseSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;
import com.amazon.iotroborunner.fmsg.utils.FmsConnectorUtils;
import com.amazon.iotroborunner.fmsg.utils.RoboRunnerUtils;
import com.amazon.iotroborunner.fmsg.utils.SimulatedFmsConnectorUtils;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This is an implementation of an FMSConnector that simulates a functionality
 * of a vendor FMS with respect to Shared Space Management application.
 * This connector does not interact with an actual vendor FMS - it only interacts
 * with IoT RoboRunner in order to mimic a real vendor FMS and vendor worker activity around
 * shared spaces. The vendor worker in this context is also the IoT RoboRunner worker.
 */
@Log4j2
public class SimulatedFmsConnector implements FmsConnector {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final long RUNNER_DELAY_IN_SECONDS = 0;
    private static final int NUM_THREADS_IN_EXECUTOR_SERVICE = 2;

    private final String fleetType;
    private final String workerFleetArn;
    private final String siteArn;
    private final RoboRunnerUtils rrUtils;
    private final AWSIoTRoboRunner rrClient;
    private final ScheduledExecutorService executor;
    private final Map<FmsCommandType, ScheduledFuture> runners;
    private final Map<FmsCommandType, List<FmsCommandCallback>> commandCallbacks;

    private boolean isSpaceManagementEnabled = false;
    private Map<String, String> workersWithLocks = new ConcurrentHashMap<>();


    /**
     * Constructs a Simulated FMS Connector.
     *
     * @param fleetConfig the provided fleet Manager configuration
     */
    public SimulatedFmsConnector(@NonNull final FmsgConnectorConfiguration fleetConfig) {
        this.rrClient = new IotRoboRunnerJavaClientProvider().getAwsIotRoboRunnerClient(fleetConfig.getAwsRegion());
        this.fleetType = fleetConfig.getFleetType();
        this.siteArn = fleetConfig.getSiteArn();
        this.workerFleetArn = fleetConfig.getWorkerFleetArn();
        this.runners = new EnumMap<>(FmsCommandType.class);
        this.commandCallbacks = new EnumMap<>(FmsCommandType.class);
        this.executor = Executors.newScheduledThreadPool(NUM_THREADS_IN_EXECUTOR_SERVICE);
        this.rrUtils = new RoboRunnerUtils(rrClient);
    }

    //////////////////////////////////////////////////////////////////////////
    // Shared Space Management Functions
    //////////////////////////////////////////////////////////////////////////

    /**
     * Configures Shared Space Management resources.
     */
    public void setupSharedSpaceManagement() {
        this.isSpaceManagementEnabled = true;
    }

    /**
     * Listens periodically and determines if there are workers waiting for a Shared Space.
     *
     * @param vendorPollingDuration customer specified polling duration
     */
    public void listenToSharedSpaces(@NonNull final Duration vendorPollingDuration) {
        FmsConnectorUtils.blockIfApplicationNotEnabled("listenToSharedSpaces",
            isSpaceManagementEnabled, FmsgApplications.SHARED_SPACE_MANAGEMENT.name());

        if (runners.containsKey(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES)) {
            log.error("Runnable already started for {} for connector {}",
                    FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES, this.fleetType);
            return;
        }

        log.info("Starting to listen to workers for connector {}", this.fleetType);

        runners.put(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES, executor.scheduleAtFixedRate(() -> {
                final List<Worker> workers = rrUtils.getWorkersInWorkerFleet(siteArn, workerFleetArn);

                for (final Worker worker : workers) {
                    final Pair<String, String> workerLocationStatusToSharedSpaceArn =
                            SimulatedFmsConnectorUtils.getWorkerLocationStatusToSharedSpaceArnMapping(worker);

                    // If the retrieved worker does not have information related to it being in proximity
                    // of a shared space, then there is no need to continue processing it.
                    if (workerLocationStatusToSharedSpaceArn == null) {
                        continue;
                    }

                    if (SimulatedFmsConnectorUtils.isWorkerWaitingForSharedSpace(
                            workerLocationStatusToSharedSpaceArn.getKey())
                        && !workersWithLocks.containsKey(worker.getArn())) {
                        requestAccessLockToSharedSpace(worker, workerLocationStatusToSharedSpaceArn.getValue());
                    } else if (workersWithLocks.containsKey(worker.getArn())
                            && workersWithLocks.get(worker.getArn()).equals(
                                    workerLocationStatusToSharedSpaceArn.getValue())
                            && SimulatedFmsConnectorUtils.isWorkerOutOfSharedSpace(
                                    workerLocationStatusToSharedSpaceArn.getKey())) {
                        requestLockReleaseFromSharedSpace(worker, workerLocationStatusToSharedSpaceArn.getValue());
                    }
                }
            },
            RUNNER_DELAY_IN_SECONDS,
            vendorPollingDuration.toSeconds(),
            TimeUnit.SECONDS
        ));
    }

    /**
     * Requests lock to access a given shared space for a given worker.
     *
     * @param worker the worker asking for shared space access
     * @param sharedSpaceArn the shared space the worker needs to go through
     */
    private void requestAccessLockToSharedSpace(@NonNull final Worker worker, @NonNull final String sharedSpaceArn) {
        final AccessSharedSpaceRequest request = SharedSpaceUtils.buildAccessSharedSpaceRequest(
                worker.getFleet(),
                worker.getArn(),
                sharedSpaceArn);

        try {
            invokeCallbacks(REQUEST_LOCK_FOR_SHARED_SPACE,
                    OBJECT_MAPPER.writeValueAsString(request));

            log.debug("Successfully requested access for worker {} and shared space {}",
                    worker.getArn(), sharedSpaceArn);
        } catch (final JsonProcessingException e) {
            log.debug("Failed to prepare access lock request for worker {} and shared space {}",
                    worker.getArn(), workersWithLocks.get(worker.getArn()));
            log.error("An error was thrown while preparing the access lock request: {}", e.getMessage());
        }
    }

    /**
     * Requests to release a lock acquired by the given worker previously for accessing the given shared space.
     *
     * @param worker the worker who has the shared space lock
     * @param sharedSpaceArn the shared space that was locked
     */
    private void requestLockReleaseFromSharedSpace(@NonNull final Worker worker, @NonNull final String sharedSpaceArn) {
        final ReleaseSharedSpaceRequest request = SharedSpaceUtils.buildReleaseSharedSpaceRequest(
                worker.getFleet(),
                worker.getArn(),
                sharedSpaceArn);

        try {
            invokeCallbacks(REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE,
                    OBJECT_MAPPER.writeValueAsString(request));

            workersWithLocks.remove(worker.getArn());

            log.debug("Successfully requested release for worker {} and shared space {}",
                    worker.getArn(), sharedSpaceArn);
        } catch (JsonProcessingException e) {
            log.debug("Failed to prepare lock release request for worker {} and shared space {}",
                    worker.getArn(), workersWithLocks.get(worker.getArn()));
            log.error("An error was thrown while preparing the lock release request: {}", e.getMessage());
        }
    }

    /**
     * Stops listening and checking whether there are any workers waiting for shared spaces.
     */
    public void stopListeningToSharedSpaces() {
        FmsConnectorUtils.blockIfApplicationNotEnabled("stopListeningToSharedSpaces",
            isSpaceManagementEnabled, FmsgApplications.SHARED_SPACE_MANAGEMENT.name());

        if (!runners.containsKey(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES)) {
            log.error("Can't stop listening to shared spaces because it was never started for {}", fleetType);
            return;
        }

        this.workersWithLocks.clear();

        log.info("Stopping shared space listening function for {}", fleetType);
        runners.remove(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES).cancel(false);
    }

    /**
     * Records the worker that is granted the shared space lock until released.
     *
     * @param workerArn the arn of the worker to allow through the shared Space
     * @param sharedSpaceArn the arn of the Shared Space to unblock
     */
    public void grantWorkerAccessToSharedSpace(@NonNull final String workerArn, @NonNull final String sharedSpaceArn) {
        workersWithLocks.put(workerArn, sharedSpaceArn);

        // Update worker in IoT RoboRunner to indicate that the worker is occupying the shared space.
        final WorkerAdditionalTransientProperties addProps = new WorkerAdditionalTransientProperties();
        final Map<String, String> workerCustomTransientProperties = new HashMap<>() {{
                put(WORKER_LOCATION_STATUS.value, IN_SHARED_SPACE.value);
                put(SHARED_SPACE_ARN.value, sharedSpaceArn);
            }};
        addProps.setCustomTransientProperties(workerCustomTransientProperties);
        addProps.setSchemaVersion(JSON_SCHEMA_VERSION);

        try {
            rrUtils.updateRoboRunnerWorkerAdditionalTransientProperties(workerArn, addProps);
            log.debug("Granted access to worker {} for shared space {}", workerArn, sharedSpaceArn);
        } catch (final JsonProcessingException e) {
            log.error("Could not update the worker with status {} for shared space {}",
                    IN_SHARED_SPACE, sharedSpaceArn);
        }

        // Since this is a Simulated Connector, there is no real FMS in which the shared space would need to be
        // unlocked for the worker to pass through. The connector would listen to the external indication that
        // the lock to the shared space must be released and will notify about it through a callback.
    }

    /**
     * Registers a callback for a specific command type.
     *
     * @param commandType the FMS command type to register the callback for
     * @param callback the callback being registered
     */
    public void registerCallback(@NonNull final FmsCommandType commandType,
                                 @NonNull final FmsCommandCallback callback) {
        log.info("Registering callback for {} for fleet type ", commandType, fleetType);

        final List<FmsCommandCallback> callbacks = commandCallbacks.getOrDefault(commandType, new ArrayList<>());
        callbacks.add(callback);

        commandCallbacks.put(commandType, callbacks);
    }

    /**
     * Unregisters a callback for a specific command type.
     *
     * @param commandType the command type to unregister a callback for
     * @param callback the callback to unregister
     */
    public void unregisterCallback(@NonNull final FmsCommandType commandType,
                                   @NonNull final FmsCommandCallback callback) {
        if (commandCallbacks.containsKey(commandType) && commandCallbacks.get(commandType).contains(callback)) {
            commandCallbacks.get(commandType).remove(callback);

            log.info("Unregistered callback for {} for fleet type {}", commandType, fleetType);
        } else {
            log.error("Callback is not registered to command type {} for fleet type ", commandType, fleetType);
        }
    }

    /**
     * Invokes all callbacks associated with the provided type by sending the provided response.
     *
     * @param type the type of the callback
     * @param response the response to sent
     */
    private void invokeCallbacks(@NonNull final FmsCommandType type, @NonNull final String response) {
        final List<FmsCommandCallback> callbacks = commandCallbacks.get(type);

        if (callbacks != null) {
            callbacks.forEach((callback) -> callback.onResponse(response));
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Worker Property Updates Methods                                      //
    //////////////////////////////////////////////////////////////////////////

    /**
     * Configures Worker Property Updates resources.
     */
    public void setupWorkerPropertyUpdates() {
        log.debug("Simulated FMS Connector does not support Worker Property Updates");
    }

    /**
     * Get the vendor worker status from the FMS.
     */
    public WorkerStatus getRobotStatusById(@NonNull final String robotId) {
        log.debug("Simulated FMS Connector does not support Worker Property Updates");
        return null;
    }

    /**
     * Function to start continuously gathering all vendor worker statuses.
     */
    public void getAllRobotStatuses() {
        log.debug("Simulated FMS Connector does not support Worker Property Updates");
    }

    /**
     * Function to stop continuously getting all vendor worker statuses.
     */
    public void stopGetAllRobotStatuses() {
        log.debug("Simulated FMS Connector does not support Worker Property Updates");
    }
}
