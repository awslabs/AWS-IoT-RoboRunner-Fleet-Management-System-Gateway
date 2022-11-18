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

import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType.REQUEST_LOCK_FOR_SHARED_SPACE;

import com.amazon.iotroborunner.fmsg.clients.IotRoboRunnerJavaClientProvider;
import com.amazon.iotroborunner.fmsg.clients.MirFmsHttpClient;
import com.amazon.iotroborunner.fmsg.clients.SecretsManagerClientProvider;
import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.constants.FmsgApplications;
import com.amazon.iotroborunner.fmsg.constants.MirApiEndpointConstants;
import com.amazon.iotroborunner.fmsg.translations.MirFmsResponseTranslator;
import com.amazon.iotroborunner.fmsg.translations.OrientationTranslation;
import com.amazon.iotroborunner.fmsg.translations.PositionTranslation;
import com.amazon.iotroborunner.fmsg.types.FmsHttpRequest;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.callback.AccessSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandCallback;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType;
import com.amazon.iotroborunner.fmsg.types.callback.ReleaseSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.mir.MirRobotStatus;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerFleetAdditionalFixedProperties;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpacePosition;
import com.amazon.iotroborunner.fmsg.utils.FmsConnectorUtils;
import com.amazon.iotroborunner.fmsg.utils.RoboRunnerUtils;
import com.amazon.iotroborunner.fmsg.utils.SecretsManagerUtils;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceClient;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * FMS Connector designed to communicate with the configured MiR FMS.
 */
@Log4j2
public class MirFmsConnector implements FmsConnector {
    private static final long RUNNER_DELAY_IN_SECONDS = 0;
    private static final long RUNNER_POLL_PERIOD_IN_SECONDS = 5;
    private static final int NUM_THREADS_IN_EXECUTOR_SERVICE = 2;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    private final String fleetType;
    private final String workerFleetArn;
    private final String siteArn;
    private final RoboRunnerUtils rrUtils;
    private final AWSIoTRoboRunner rrClient;
    private final MirFmsHttpClient fmsClient;
    private final Map<String, String> robotIdToArn;
    private final Map<String, String> robotArnToId;
    private final ScheduledExecutorService executor;
    private final Map<FmsCommandType, ScheduledFuture> runners;
    private final Map<FmsCommandType, List<FmsCommandCallback>> commandCallbacks;

    // Shared Space Management Resources
    private boolean isSpaceManagementEnabled = false;
    private SharedSpaceClient sharedSpaceClient = null;
    private Map<String, String> sharedSpaceArnToId = null;
    private Map<String, String> sharedSpaceIdToArn = null;
    private Map<String, Pair<String, Point>> sharedSpaceArnToLockHoldingWorkerArn = null;
    private Map<String, SharedSpacePosition> sharedSpaceIdToPosition = null;

    // Worker Property Updates Resources
    private boolean isWorkerPropertyUpdatesEnabled = true;
    private MirFmsResponseTranslator responseTranslator = null;
    private PositionTranslation positionTranslation = null;
    private OrientationTranslation orientationTranslation = null;


    /**
     * MiR FMS Connector.
     *
     * @param fleetConfig Fleet Manager Configs.
     */
    public MirFmsConnector(@NonNull final FmsgConnectorConfiguration fleetConfig) {

        this.rrClient =
            new IotRoboRunnerJavaClientProvider().getAwsIotRoboRunnerClient(fleetConfig.getAwsRegion());
        final String authSecretValue = SecretsManagerUtils.getSecret(
            new SecretsManagerClientProvider().getAwsSecretsManagerClient(fleetConfig.getAwsRegion()),
            fleetConfig.getApiSecretName());

        this.fleetType = fleetConfig.getFleetType();
        this.siteArn = fleetConfig.getSiteArn();
        this.workerFleetArn = fleetConfig.getWorkerFleetArn();
        this.runners = new EnumMap<>(FmsCommandType.class);
        this.commandCallbacks = new EnumMap<>(FmsCommandType.class);
        this.executor = Executors.newScheduledThreadPool(NUM_THREADS_IN_EXECUTOR_SERVICE);
        this.rrUtils = new RoboRunnerUtils(rrClient);
        this.robotIdToArn = rrUtils.createRobotIdToWorkerArnMap(fleetConfig.getSiteArn(),
            fleetConfig.getWorkerFleetArn());
        this.robotArnToId = this.robotIdToArn.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.fmsClient = new MirFmsHttpClient(fleetConfig.getApiEndpoint(), authSecretValue);
    }

    /**
     * Registers a callback for the provided FMS command type.
     *
     * @param commandType fms command type to register the callback for
     * @param callback    callback being registered
     */
    public void registerCallback(@NonNull final FmsCommandType commandType,
                                 @NonNull final FmsCommandCallback callback) {
        log.info("Registering callback for " + commandType + " for fleet type " + fleetType);
        final List<FmsCommandCallback> callbacks = commandCallbacks.getOrDefault(commandType, new ArrayList<>());
        callbacks.add(callback);
        commandCallbacks.put(commandType, callbacks);
    }

    /**
     * Unregisters a callback for the provided FMS command type.
     *
     * @param commandType command type to unregister a callback for
     * @param callback    callback to unregister
     */
    public void unregisterCallback(@NonNull final FmsCommandType commandType,
                                   @NonNull final FmsCommandCallback callback) {
        if (commandCallbacks.containsKey(commandType) && commandCallbacks.get(commandType).contains(callback)) {
            commandCallbacks.get(commandType).remove(callback);
            log.info("Unregistered callback for " + commandType + " for fleet type " + fleetType);
        } else {
            log.error("Callback is not registered to command type " + commandType + " for fleet type " + fleetType);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Worker Property Updates Functions
    //////////////////////////////////////////////////////////////////////////

    /**
     * Configures Worker Property Updates resources.
     */
    public void setupWorkerPropertyUpdates() {
        this.responseTranslator = new MirFmsResponseTranslator();
        this.isWorkerPropertyUpdatesEnabled = true;

        final Optional<WorkerFleetAdditionalFixedProperties> properties =
            rrUtils.getWorkerFleetAdditionalFixedProperties(this.workerFleetArn);

        if (properties.isPresent()) {
            final WorkerFleetAdditionalFixedProperties workerFleetProperties = properties.get();
            if (workerFleetProperties.getPositionConversion() != null) {
                log.info("Position conversion configuration provided"
                    + " - position co-ordinates will be transformed");
                this.positionTranslation = new PositionTranslation(workerFleetProperties.getPositionConversion());
            }
            if (workerFleetProperties.getOrientationOffset() != null) {
                log.info("Orientation offset configuration provided - orientation value will be transformed");
                this.orientationTranslation = new OrientationTranslation(workerFleetProperties.getOrientationOffset());
            }
        }
    }

    /**
     * Get the robot status from the FMS.
     *
     * @param robotId identifier of the robot to get the status of
     * @return worker status object created from the FMS response
     */
    public WorkerStatus getRobotStatusById(@NonNull final String robotId) {
        final String apiEndpoint = MirApiEndpointConstants.getRobotStatusEndpoint(robotId);
        final String response = fmsClient.sendFmsRequest(new FmsHttpRequest("GET", apiEndpoint, ""));

        try {
            return this.responseTranslator.getWorkerStatusFromFmsResponse(
                robotId,
                response,
                this.positionTranslation,
                this.orientationTranslation);
        } catch (Exception e) {
            log.error("Error received when converting from FmsResponse to WorkerStatus", e);
            return null;
        }
    }

    /**
     * Function to start continuously gathering all robot statuses.
     */
    public void getAllRobotStatuses() {
        FmsConnectorUtils.blockIfApplicationNotEnabled("getAllRobotStatuses",
            isWorkerPropertyUpdatesEnabled, FmsgApplications.WORKER_PROPERTY_UPDATES.name());

        if (runners.containsKey(FmsCommandType.GET_STATUS)) {
            log.error("Runnable already started for {} for fleet type {}.", FmsCommandType.GET_STATUS, fleetType);
        } else {
            log.info("Starting to get all robot statuses continuously for fleet type " + fleetType);
            runners.put(FmsCommandType.GET_STATUS, executor.scheduleAtFixedRate(
                () -> {
                    for (final Map.Entry<String, String> robot : this.robotIdToArn.entrySet()) {
                        final WorkerStatus status = getRobotStatusById(robot.getKey());
                        if (status != null) {
                            this.rrUtils.updateRoboRunnerWorkerStatus(robot.getValue(), status);
                        }
                    }
                },
                RUNNER_DELAY_IN_SECONDS,
                RUNNER_POLL_PERIOD_IN_SECONDS,
                TimeUnit.SECONDS
            ));
        }
    }

    /**
     * Function to stop continuously getting all robot statuses.
     */
    public void stopGetAllRobotStatuses() {
        FmsConnectorUtils.blockIfApplicationNotEnabled("stopGetAllRobotStatuses",
            isWorkerPropertyUpdatesEnabled, FmsgApplications.WORKER_PROPERTY_UPDATES.name());

        log.info("Preparing to stop gathering all robot statuses for fleet type " + fleetType);
        if (runners.containsKey(FmsCommandType.GET_STATUS)) {
            log.info("Stopping gathering all robot statuses for fleet type " + fleetType);
            runners.remove(FmsCommandType.GET_STATUS).cancel(false);
        } else {
            log.error("Gathering of all robot statuses has not been started for fleet type " + fleetType);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Shared Space Management Functions
    //////////////////////////////////////////////////////////////////////////

    /**
     * Configures the resources required to run Shared Space Management.
     */
    public void setupSharedSpaceManagement() {
        this.sharedSpaceClient = new SharedSpaceClient(rrClient);
        configureSharedSpaceLocalStorage();
        blockAllSharedSpaces(this.sharedSpaceArnToId.values());
        this.isSpaceManagementEnabled = true;
    }

    /**
     * Listens to vendor Shared Spaces to determine if any robots that have entered or exited. Findings are reported
     * directly to the Shared Space Management application to allow for arbitration.
     *
     * @param vendorPollingDuration customer specified polling duration
     */
    public void listenToSharedSpaces(@NonNull final Duration vendorPollingDuration) {
        FmsConnectorUtils.blockIfApplicationNotEnabled("listenToSharedSpaces",
            isSpaceManagementEnabled, FmsgApplications.SHARED_SPACE_MANAGEMENT.name());

        if (runners.containsKey(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES)) {
            log.error("Runnable already started for {} for connector {}",
                FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES,
                this.fleetType);
            return;
        }
        log.info("Starting to listen to shared spaces with {} worker(s) for connector {}",
            this.robotIdToArn.size(), this.fleetType);
        runners.put(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES, executor.scheduleAtFixedRate(
            () -> {
                for (final String robotId : this.robotIdToArn.keySet()) {
                    if (robotIsWaitingForSharedSpace(robotId)) {
                        extractRobotVendorPositionPoint(robotId).ifPresent(robotPosition -> {
                            requestSharedSpaceIfWithinTwoMeters(robotId, robotPosition);
                        });
                    }
                }
            },
            RUNNER_DELAY_IN_SECONDS,
            vendorPollingDuration.toSeconds(),
            TimeUnit.SECONDS
        ));
        runners.put(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACE_EXITS, executor.scheduleAtFixedRate(
            this::monitorSharedSpaceExits,
            RUNNER_DELAY_IN_SECONDS,
            vendorPollingDuration.toSeconds(),
            TimeUnit.SECONDS
        ));
    }

    /**
     * Terminates the connector's shared space listening function.
     */
    public void stopListeningToSharedSpaces() {
        FmsConnectorUtils.blockIfApplicationNotEnabled("stopListeningToSharedSpaces",
            isSpaceManagementEnabled, FmsgApplications.SHARED_SPACE_MANAGEMENT.name());

        log.info("Preparing to stop shared space listening function for {}", fleetType);
        if (!runners.containsKey(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES)) {
            log.error("Can't stop listening to shared spaces because it was never started for {}", fleetType);
            return;
        }
        log.info("Stopping shared space listening function for {}", fleetType);
        runners.remove(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACES).cancel(false);
        runners.remove(FmsCommandType.LISTEN_TO_VENDOR_SHARED_SPACE_EXITS).cancel(false);
    }

    /**
     * Grants the robot access into the vendor shared space by unblocking the vendor shared space (i.e. limit robot
     * zone). In doing so, the robot is free to travel across the shared space. The shared space is immediately blocked
     * again to prevent unplanned entries.
     *
     * @param workerArn      arn of the RoboRunner Worker (robot) to let into the shared space
     * @param sharedSpaceArn arn of the shared space to let the worker (robot) in
     */
    public void grantWorkerAccessToSharedSpace(@NonNull final String workerArn, @NonNull final String sharedSpaceArn) {
        FmsConnectorUtils.blockIfApplicationNotEnabled("grantWorkerAccessToSharedSpace",
            isSpaceManagementEnabled, FmsgApplications.SHARED_SPACE_MANAGEMENT.name());

        final String robotId = this.robotArnToId.get(workerArn);
        final Pair<String, Point> lockHolder
            = this.sharedSpaceArnToLockHoldingWorkerArn.getOrDefault(sharedSpaceArn, null);

        if (lockHolder != null && lockHolder.getLeft().equals(robotId)) {
            log.info("Robot " + robotId + " has already been granted shared space: " + sharedSpaceArn);
            return;
        }

        log.info("Attempting to unblock shared space: {} for worker: {}", sharedSpaceArn, workerArn);

        final String guid = this.sharedSpaceArnToId.get(sharedSpaceArn);
        final String blockEndpoint = MirApiEndpointConstants.getBlockedSharedSpaceEndpoint(guid);
        try {
            final String unblockResponse =
                fmsClient.sendFmsRequest(new FmsHttpRequest("PUT", blockEndpoint, "{ \"block\": false}"));
            log.debug("Received response {} from the vendor FMS.", unblockResponse);

            if (unblockResponse == null) {
                final String failureResponse = String.format(
                    "[FAILURE] Unable to unblock shared space: %s for worker: %s",
                    sharedSpaceArn, workerArn);
                log.error(failureResponse);
                invokeCallbacks(FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE, failureResponse);
                return;
            }
            log.info("Unblocked shared space: {} for worker: {}", sharedSpaceArn, workerArn);
            extractRobotVendorPositionPoint(robotId).ifPresent(entryPosition -> {
                    final Pair<String, Point> workerEntryPoint = ImmutablePair.of(robotId, entryPosition);
                    this.sharedSpaceArnToLockHoldingWorkerArn.put(sharedSpaceArn, workerEntryPoint);
                    log.debug("Added worker as lock holder in the location storage.");
                }
            );
            Thread.sleep(1000);
        } catch (Exception e) {
            log.error("Failed to unblock shared space: {} for worker: {}", sharedSpaceArn, workerArn);
        } finally {
            fmsClient.sendFmsRequest(new FmsHttpRequest("PUT", blockEndpoint, "{ \"block\": true}"));
            log.info("Blocked shared space: {}", sharedSpaceArn);
        }
    }

    /**
     * Initializes the local storage required to run Shared Space Management. Required storage focuses on maps
     * which tell us: how RoboRunner Shared Space ARNs map to vendor shared space ids and the position of each of these
     * vendor shared spaces. All storage must be initialized together to ensure that we only store metadata for
     * shared spaces that meet the Shared Space Management validation requirement of existing in the vendor FMS with
     * a physical position.
     */
    protected void configureSharedSpaceLocalStorage() {
        try {
            log.info("Configuring Shared Space Management Local Storage");
            this.sharedSpaceArnToId = new ConcurrentHashMap<>();
            this.sharedSpaceIdToArn = new ConcurrentHashMap<>();
            this.sharedSpaceIdToPosition = new ConcurrentHashMap<>();
            this.sharedSpaceArnToLockHoldingWorkerArn = new ConcurrentHashMap<>();
            final List<SharedSpace> rrSharedSpaces = this.sharedSpaceClient.getAllSharedSpaces(siteArn, workerFleetArn);
            log.debug("Found {} RoboRunner shared spaces", rrSharedSpaces.size());

            for (final SharedSpace rrSharedSpace : rrSharedSpaces) {
                log.info("Gathering Shared Space metadata for shared space: {} from the vendor FMS: {}",
                    rrSharedSpace.getDestinationArn(),
                    fleetType);
                final String sharedSpaceArn = rrSharedSpace.getDestinationArn();
                final String sharedSpaceId = rrSharedSpace.getVendorSharedSpace().getGuid();
                getSharedSpacePositions(sharedSpaceId).ifPresent(position -> {
                    log.info("Shared space exists in the vendor FMS. shared space: {}", sharedSpaceArn);
                    final Optional<Polygon> possiblePolygon =
                        SharedSpaceUtils.createSharedSpacePolygon(position.getCoordinates());

                    if (possiblePolygon.isPresent()) {
                        log.info("Position coordinates found for shared space: {}", sharedSpaceArn);
                        position.setPositionPolygon(possiblePolygon.get());

                        log.info("Successfully validated shared space: {} and will add it to local storage",
                            rrSharedSpace.getDestinationArn());
                        this.sharedSpaceIdToPosition.put(sharedSpaceId, position);
                        this.sharedSpaceIdToArn.put(sharedSpaceId, sharedSpaceArn);
                        this.sharedSpaceArnToId.put(sharedSpaceArn, sharedSpaceId);
                        log.debug("Id: {}, Position: {}, shared space: {}", sharedSpaceId, position, sharedSpaceArn);
                    }
                });
            }
        } catch (final JsonProcessingException e) {
            log.error("[FAILURE] Unable to get and process RoboRunner Shared Spaces for site: {}, "
                + "workerFleet: {}", siteArn, workerFleetArn);
        }
    }

    /**
     * Listens to the vendor shared spaces to see if the robots crossing them (a.k.a. lock holders) have exited. A MiR
     * robot is considered having exited a shared space if (1) the robot isn't current in the shared space and (2)
     * the robot's current position isn't at the entry point. If a robot has exited a shared space, this function will
     * automatically report this information to the Shared Space Management app using a callback.
     */
    protected void monitorSharedSpaceExits() {
        for (final Map.Entry<String, Pair<String, Point>> entry :
            this.sharedSpaceArnToLockHoldingWorkerArn.entrySet()) {
            final String robotId = entry.getValue().getLeft();
            final Point entryPoint = entry.getValue().getRight();
            final String sharedSpaceId = this.sharedSpaceArnToId.get(entry.getKey());
            extractRobotVendorPositionPoint(robotId).ifPresent(currentRobotPosition -> {
                    final boolean isWithinSharedSpace = currentRobotPosition.within(
                        this.sharedSpaceIdToPosition.get(sharedSpaceId).getPositionPolygon());
                    if (isWithinSharedSpace) {
                        log.info("Robot {} still within the shared space", robotId);
                        return;
                    }

                    log.debug("Current Position {}, Entry Point {}", currentRobotPosition, entryPoint);
                    final double distanceFromEntry = Math.pow(currentRobotPosition.getX() - entryPoint.getX(), 2)
                        + Math.pow(currentRobotPosition.getY() - entryPoint.getY(), 2);
                    log.debug("Distance from entry {}", distanceFromEntry);

                    final boolean insideEntryRadius = distanceFromEntry < Math.pow(2, 2);
                    if (insideEntryRadius) {
                        log.info("Robot {} is still entering the shared space", robotId);
                        return;
                    }
                    final ReleaseSharedSpaceRequest request = ReleaseSharedSpaceRequest.builder()
                        .workerArn(this.robotIdToArn.get(robotId))
                        .sharedSpaceArn(entry.getKey())
                        .workerFleetArn(this.workerFleetArn)
                        .releaseTime(Instant.now().toEpochMilli())
                        .build();
                    try {
                        log.debug("Notifying SM app that robot {} has exited shared space {}",
                            robotId, entry.getKey());
                        invokeCallbacks(FmsCommandType.REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE,
                            OBJECT_MAPPER.writeValueAsString(request));
                        log.debug("Notified SM app that robot exited the shared space");
                        this.sharedSpaceArnToLockHoldingWorkerArn.remove(entry.getKey());
                    } catch (final JsonProcessingException e) {
                        log.error("Unable to notify SM application of shared space exit", e);
                    }
                }
            );
        }
    }

    /**
     * Checks if the given robot is waiting for a shared space. A MiR robot is considered to be waiting for a shared
     * space is the mission text in the robot's status explicitly states it is.
     *
     * @param robotId identifier of the robot we're checking to see if it's waiting
     * @return Boolean true if waiting for a shared space, else false
     */
    protected boolean robotIsWaitingForSharedSpace(@NonNull final String robotId) {
        final String apiEndpoint = MirApiEndpointConstants.getRobotStatusEndpoint(robotId);
        final String response = fmsClient.sendFmsRequest(new FmsHttpRequest("GET", apiEndpoint, ""));
        log.debug("Requested the worker status for {} and received FMS response: {}", robotId, response);

        try {
            final String missionTextNode = OBJECT_MAPPER.readTree(response)
                .get("status")
                .get("mission_text").toString();

            if (missionTextNode.contains("Waiting to be assigned a necessary resource by MiR Fleet.")) {
                return true;
            }
        } catch (final Exception e) {
            log.error("Error received when extracting robot mission_text from worker status string: " + response, e);
        }
        return false;
    }

    /**
     * Extracts the vendor shared space physical coordinates from the vendor FMS and then
     * translates them into a SharedSpacePosition object storing the polygon coordinates.
     *
     * @param guid identifier of the shared space we're getting the position of
     * @return optional of the SharedSpacePosition object storing position as polygon coordinates
     */
    protected Optional<SharedSpacePosition> getSharedSpacePositions(@NonNull final String guid) {
        final String apiEndpoint = MirApiEndpointConstants.getSharedSpaceEndpoint(guid);
        final String response = fmsClient.sendFmsRequest(new FmsHttpRequest("GET", apiEndpoint, ""));
        log.debug("Requested the shared space position for {} and received FMS response: {}", guid, response);
        if (response == null) {
            log.error("[FAILURE] Unable to locate Shared Space in MiR FMS so no position could be extracted.");
            return Optional.empty();
        }

        final SharedSpacePosition sharedSpacePosition = new SharedSpacePosition();
        try {
            final JsonNode polygonNode = OBJECT_MAPPER.readTree(response).get("polygon");

            polygonNode.forEach((subResponseNode) -> {
                final double xCoord = subResponseNode.get("x").asDouble();
                final double yCoord = subResponseNode.get("y").asDouble();
                sharedSpacePosition.addCoordinates(xCoord, yCoord);
            });

            return Optional.of(sharedSpacePosition);
        } catch (final Exception e) {
            log.error("Error received when converting from FmsResponse to Shared Space Polygon", e);
            return Optional.empty();
        }
    }

    /**
     * Extracts the current x and y vendor coordinates for the requested robot.
     *
     * @param robotId robot identifier to retrieve position of
     * @return optional of the Point object containing the robot's x and y coordinates
     */
    protected Optional<Point> extractRobotVendorPositionPoint(@NonNull final String robotId) {
        final String apiEndpoint = MirApiEndpointConstants.getRobotStatusEndpoint(robotId);
        final String response = fmsClient.sendFmsRequest(new FmsHttpRequest("GET", apiEndpoint, ""));
        log.debug("Requested the robot vendor position for {} and received FMS response: {}", robotId, response);
        if (response == null) {
            log.error("[FAILURE] Unable to locate robot in MiR FMS so no position could be extracted.");
            return Optional.empty();
        }
        try {
            final MirRobotStatus status = OBJECT_MAPPER.readValue(response, MirRobotStatus.class);
            return Optional.of(GEOMETRY_FACTORY.createPoint(new Coordinate(status.getRobotX(), status.getRobotY())));
        } catch (final Exception e) {
            log.error("Error received when extracting robot position point from worker response string: "
                + response, e);
            return Optional.empty();
        }
    }

    /**
     * Blocks the provided vendor shared spaces (i.e. limit robot zones). Assuming the shared space is already
     * in a blocked state, making this call will have no affect as the MiR FMS will simply ignore it.
     *
     * @param sharedSpaceIds list of shared spaces to block
     */
    private void blockAllSharedSpaces(@NonNull final Collection<String> sharedSpaceIds) {
        for (final String sharedSpaceId : sharedSpaceIds) {
            log.debug("Blocking Shared Space {} for {}", sharedSpaceId, this.fleetType);
            final String blockEndpoint = MirApiEndpointConstants.getBlockedSharedSpaceEndpoint(sharedSpaceId);
            fmsClient.sendFmsRequest(new FmsHttpRequest("PUT", blockEndpoint, "{ \"block\": true}"));
        }
    }

    /**
     * Determines if a robot is waiting for a vendor shared space. A robot is considered to be waiting for the
     * shared spaced space if it's no more than 2 meters outside the shared space. Robots waiting to a shared space are
     * added to the Shared Space Management priority queue via a callback.
     *
     * @param robotId       robot (worker) identifier we're checking
     * @param robotPosition physical position of the robot (worker) according to the FMS
     */
    private void requestSharedSpaceIfWithinTwoMeters(@NonNull final String robotId,
                                                     @NonNull final Point robotPosition) {
        for (final Map.Entry<String, SharedSpacePosition> entry : this.sharedSpaceIdToPosition.entrySet()) {
            final Boolean isWithinTwoMeters =
                SharedSpaceUtils.robotIsWithinTwoMeters(robotPosition, entry.getValue().getPositionPolygon());

            if (isWithinTwoMeters) {
                final AccessSharedSpaceRequest request = AccessSharedSpaceRequest.builder()
                    .workerFleetArn(this.workerFleetArn)
                    .sharedSpaceArn(this.sharedSpaceIdToArn.get(entry.getKey()))
                    .workerArn(this.robotIdToArn.get(robotId))
                    .requestTime(Instant.now().toEpochMilli())
                    .build();
                try {
                    invokeCallbacks(REQUEST_LOCK_FOR_SHARED_SPACE, OBJECT_MAPPER.writeValueAsString(request));
                } catch (final JsonProcessingException e) {
                    log.error("Unable to convert access shared space request to string. {}",
                        e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * Invokes all callbacks associated with the provided type by sending the provided response.
     *
     * @param type     callback Type
     * @param response response to send
     */
    private void invokeCallbacks(@NonNull final FmsCommandType type, @NonNull final String response) {
        final List<FmsCommandCallback> callbacks = commandCallbacks.get(type);

        if (callbacks != null) {
            callbacks.forEach((callback) -> callback.onResponse(response));
        }
    }
}
