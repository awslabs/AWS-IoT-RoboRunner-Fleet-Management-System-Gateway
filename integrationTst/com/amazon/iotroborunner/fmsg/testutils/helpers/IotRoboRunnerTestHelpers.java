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

package com.amazon.iotroborunner.fmsg.testutils.helpers;


import static com.amazon.iotroborunner.fmsg.constants.IotRoboRunnerWorkerResourcePropertyConstants.VendorPropertiesState.EXECUTING;
import static com.amazon.iotroborunner.fmsg.constants.IotRoboRunnerWorkerResourcePropertyConstants.VendorPropertiesState.READY;
import static com.amazon.iotroborunner.fmsg.constants.RoboRunnerWorkerStatusConstants.JSON_SCHEMA_VERSION;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.SHARED_SPACE_ARN;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.config.IntegrationTestPropertyConfiguration;
import com.amazon.iotroborunner.fmsg.testutils.providers.ClientProvider;
import com.amazon.iotroborunner.fmsg.testutils.providers.ConfigurationProvider;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.types.roborunner.VendorAdditionalTransientProperties;
import com.amazon.iotroborunner.fmsg.types.roborunner.WorkerAdditionalTransientProperties;
import com.amazon.iotroborunner.fmsg.types.sharedspace.DestinationAdditionalInformation;
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.GetDestinationRequest;
import com.amazonaws.services.iotroborunner.model.GetDestinationResult;
import com.amazonaws.services.iotroborunner.model.GetWorkerRequest;
import com.amazonaws.services.iotroborunner.model.GetWorkerResult;
import com.amazonaws.services.iotroborunner.model.ListDestinationsRequest;
import com.amazonaws.services.iotroborunner.model.ListWorkersRequest;
import com.amazonaws.services.iotroborunner.model.Orientation;
import com.amazonaws.services.iotroborunner.model.PositionCoordinates;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerRequest;
import com.amazonaws.services.iotroborunner.model.UpdateWorkerResult;
import com.amazonaws.services.iotroborunner.model.VendorProperties;
import com.amazonaws.services.iotroborunner.model.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Collection of helper methods to assist with integration test logic
 * specifically when dealing with IoT RoboRunner resources.
 */
public final class IotRoboRunnerTestHelpers {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final double DOUBLE_VALUE_COMPARISON_ALLOWED_ERROR = 0.000001d;

    private static final IntegrationTestPropertyConfiguration integTestPropertyConfig =
            ConfigurationProvider.getIntegrationTestPropertyConfigs().get(RobotFleetType.MIR.value);
    private static AWSIoTRoboRunner roboRunnerClient = ClientProvider.getRoboRunnerClient();

    //////////////////////////////////////////////////////////////////////////
    ///                          GETTER HELPERS                            ///
    //////////////////////////////////////////////////////////////////////////

    /**
     * Acquires the test worker from IoT RoboRunner and creates
     * a vendor worker id to corresponding worker arn mapping.
     *
     * @param siteArn the unique identifier of a site
     * @param fleetArn the unique identifier of a worker fleet under site with siteArn
     * @return the pair representing a mapping between vendor worker id
     *         from MiR FMS and corresponding worker arn from IoT RoboRunner
     */
    public static Pair<Integer, String> getVendorWorkerIdToWorkerArnMapping(
            @NonNull final String siteArn,
            @NonNull final String fleetArn) {
        final Worker worker = getWorker(siteArn, fleetArn);
        final VendorProperties vendorProperties = worker.getVendorProperties();

        return Pair.of(Integer.parseInt(vendorProperties.getVendorWorkerId()), worker.getArn());
    }

    /**
     * Acquires the test worker from IoT RoboRunner.
     *
     * @param siteArn the unique identifier of a site
     * @param fleetArn the unique identifier of a worker fleet under site with siteArn
     * @return the worker acquired based on given site and fleet
     */
    public static Worker getWorker(
            @NonNull final String siteArn,
            @NonNull final String fleetArn) {
        final List<Worker> workers = roboRunnerClient.listWorkers(new ListWorkersRequest()
                .withFleet(fleetArn)
                .withSite(siteArn)).getWorkers();

        assertTrue(workers.size() == 1);

        return workers.get(0);
    }

    /**
     * Queries and returns the Worker from IoT RoboRunner with given worker arn.
     *
     * @param workerArn the unique identifier of a worker in IoT RoboRunner
     * @return the result of acquiring the worker by id
     */
    public static GetWorkerResult getWorkerById(@NonNull final String workerArn) {
        return roboRunnerClient.getWorker(new GetWorkerRequest()
                .withId(workerArn));
    }

    /**
     * Extracts the custom properties from worker additional transient properties.
     *
     * @param workerAdditionalTransientPropertiesStr the worker additional transient properties represented as String
     * @return the map of additional transient custom properties of a worker
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static Map<String, String> getWorkerAdditionalTransientCustomProperties(
            @NonNull final String workerAdditionalTransientPropertiesStr) throws JsonProcessingException {
        final WorkerAdditionalTransientProperties workerAdditionalTransientProperties =
                WorkerAdditionalTransientProperties.readWorkerAdditionalTransientProperties(
                        workerAdditionalTransientPropertiesStr);

        return workerAdditionalTransientProperties.getCustomTransientProperties();
    }

    /**
     * Acquires the test destination from IoT RoboRunner which represents the shared space.
     *
     * @param siteArn the unique identifier of a site
     * @return the destination acquired based on given site
     */
    public static Destination getDestination(@NonNull final String siteArn) {
        final List<Destination> destinations = roboRunnerClient.listDestinations(new ListDestinationsRequest()
                .withSite(siteArn)).getDestinations();

        assertTrue(destinations.size() == 1);

        return destinations.get(0);
    }

    /**
     * Acquires the destination resource from IoT RoboRunner.
     *
     * @param destinationArn the unique identifier of destination
     * @return the result of the IoT RoboRunner query to acquire the destination
     */
    public static GetDestinationResult getDestinationById(@NonNull final String destinationArn) {
        return roboRunnerClient.getDestination(new GetDestinationRequest()
                .withId(destinationArn));
    }

    /**
     * Acquires the test shared space from IoT RoboRunner and creates
     * a shared space id to corresponding shared space arn mapping.
     *
     * @param siteArn the unique identifier of a site
     * @param fleetArn the unique identifier of a worker fleet under site with siteArn
     * @return the pair representing a mapping between shared space id from vendor FMS
     *         and corresponding shared space arn from IoT RoboRunner
     * @throws JsonProcessingException if there is an issue reading the JSON String
     * @throws InvalidPropertiesFormatException if shared space is incorrectly formatted
     */
    public static Pair<String, String> getSharedSpaceArnToVendorSharedSpaceIdMapping(
            @NonNull final String siteArn,
            @NonNull final String fleetArn) throws JsonProcessingException, InvalidPropertiesFormatException {

        final Destination destination = getDestination(siteArn);
        final String additionalFixedPropertiesStr = destination.getAdditionalFixedProperties();
        final DestinationAdditionalInformation destinationAdditionalInformation = OBJECT_MAPPER
                .readValue(additionalFixedPropertiesStr, DestinationAdditionalInformation.class);

        final List<VendorSharedSpace> vendorSharedSpaces = destinationAdditionalInformation.getVendorSharedSpaces();
        String sharedSpaceId = null;
        for (VendorSharedSpace vendorSharedSpace : vendorSharedSpaces) {
            if (vendorSharedSpace.getWorkerFleet().equals(fleetArn)) {
                sharedSpaceId = vendorSharedSpace.getGuid();
            }
        }

        if (sharedSpaceId == null) {
            throw new InvalidPropertiesFormatException(
                    String.format("Cannot find guid for MiR shared space %s", destination.getArn()));
        }

        return Pair.of(destination.getArn(), sharedSpaceId);
    }

    /**
     * Given a series of single Worker resource updates that were acquired over a period of time from IoT RoboRunner,
     * returns a series of vendor worker properties from worker resources accordingly. The map represents updated time
     * to worker resource update data mappings.
     *
     * @param workerUpdateHistory the map that contains the series of Worker resource updates ordered by time
     * @return the map with the series of vendor worker property updates ordered by time
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static NavigableMap<String, VendorProperties> getUpdatedTimeToWorkerVendorPropertiesMap(
            @NonNull final NavigableMap<String, GetWorkerResult> workerUpdateHistory) throws JsonProcessingException {


        final NavigableMap<String, VendorProperties> updatedTimeToVendorPropertiesMap = new TreeMap<>();

        for (final Map.Entry<String, GetWorkerResult> workerInfo : workerUpdateHistory.entrySet()) {
            updatedTimeToVendorPropertiesMap.put(workerInfo.getKey(), workerInfo.getValue().getVendorProperties());
        }

        return updatedTimeToVendorPropertiesMap;
    }

    /**
     * Given vendor worker properties from IoT RoboRunner Worker resource,
     * extracts the coordinates of the vendor.
     *
     * @param vendorProperties the vendor worker properties from IoT RoboRunner
     * @return the coordinates of the vendor worker
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static PositionCoordinates getVendorWorkerPosition(@NonNull final VendorProperties vendorProperties)
            throws JsonProcessingException {
        final VendorAdditionalTransientProperties vendorAdditionalTransientProperties =
                VendorAdditionalTransientProperties.readVendorAdditionalTransientProperties(
                        vendorProperties.getVendorAdditionalTransientProperties());

        return vendorAdditionalTransientProperties.getVendorPosition();
    }

    /**
     * Given vendor worker properties from IoT RoboRunner Worker resource,
     * extracts the state of the vendor worker.
     *
     * @param vendorProperties the vendor worker properties from IoT RoboRunner
     * @return the state of vendor worker
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static String getVendorWorkerState(@NonNull final VendorProperties vendorProperties)
            throws JsonProcessingException {
        final VendorAdditionalTransientProperties vendorAdditionalTransientProperties =
                VendorAdditionalTransientProperties.readVendorAdditionalTransientProperties(
                        vendorProperties.getVendorAdditionalTransientProperties());

        return vendorAdditionalTransientProperties.getVendorState();
    }

    /**
     * Given vendor worker properties from IoT RoboRunner Worker resource,
     * extracts the orientation of the vendor worker.
     *
     * @param vendorProperties the vendor worker properties from IoT RoboRunner
     * @return the orientation of vendor worker
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static Orientation getVendorWorkerOrientation(@NonNull final VendorProperties vendorProperties)
            throws JsonProcessingException {
        final VendorAdditionalTransientProperties vendorAdditionalTransientProperties =
                VendorAdditionalTransientProperties.readVendorAdditionalTransientProperties(
                        vendorProperties.getVendorAdditionalTransientProperties());

        return vendorAdditionalTransientProperties.getVendorOrientation();
    }

    /**
     * Given a list of Worker resource data (sorted by time of update), extracts the updatedAt
     * property for each worker resource and calculates the total time between updates.
     *
     * @param workerUpdateInfo the list of worker resource records collected over time
     * @return the list with durations between worker updates
     */
    public static List<Duration> getDurationBetweenWorkerUpdates(
            @NonNull final List<GetWorkerResult> workerUpdateInfo) {
        assertFalse(workerUpdateInfo.isEmpty() && workerUpdateInfo.size() == 1);

        final List<Date> updatedAtValues = workerUpdateInfo.stream()
                .filter(worker -> isVendorWorkerExecuting(worker.getVendorProperties()))
                .map(GetWorkerResult::getUpdatedAt)
                .collect(Collectors.toList());

        Collections.sort(updatedAtValues);

        return IntStream
                .range(0, updatedAtValues.size() - 1)
                .mapToObj(index -> Duration.between(
                        workerUpdateInfo.get(index).getUpdatedAt().toInstant(),
                        workerUpdateInfo.get(index + 1).getUpdatedAt().toInstant()))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the orientation property for each worker resource into a list
     * given a list of Worker resources (sorted by time of update).
     *
     * @param workerUpdateInfo the list of worker resource records collected over time
     * @return the list of orientation values, empty list otherwise
     */
    public static List<Double> getOrientationTimeSeries(@NonNull final List<GetWorkerResult> workerUpdateInfo)
            throws JsonProcessingException {
        assertFalse(workerUpdateInfo.isEmpty() || workerUpdateInfo.size() == 1);

        final List<Double> orientationTimeSeries = new ArrayList<>();

        for (final GetWorkerResult workerInfo : workerUpdateInfo) {
            if (isVendorWorkerExecuting(workerInfo.getVendorProperties())) {
                orientationTimeSeries.add(getVendorWorkerOrientation(workerInfo.getVendorProperties()).getDegrees());
            }
        }

        Collections.sort(orientationTimeSeries);

        return orientationTimeSeries;
    }

    //////////////////////////////////////////////////////////////////////////
    ///                          UPDATE HELPERS                            ///
    //////////////////////////////////////////////////////////////////////////

    /**
     * Updates worker in IoT RoboRunner by given worker arn specifically to indicate either that
     * worker is in front of the shared space, in shared space, or out of the shared space.
     *
     * @param workerArn the unique identifier of the worker
     * @param sharedSpaceArn the unique identifier of shared space in IoT RoboRunner
     * @param workerLocationStatus the location status of the worker
     * @return the result of the update
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static UpdateWorkerResult updateWorker(@NonNull final String workerArn,
                                                  @NonNull final String sharedSpaceArn,
                                                  @NonNull final String workerLocationStatus)
            throws JsonProcessingException {
        final WorkerAdditionalTransientProperties addProps = new WorkerAdditionalTransientProperties();
        final Map<String, String> workerCustomTransientProperties = new HashMap<>() {{
                put(WORKER_LOCATION_STATUS.value, workerLocationStatus);
                put(SHARED_SPACE_ARN.value, sharedSpaceArn);
            }};

        addProps.setCustomTransientProperties(workerCustomTransientProperties);
        addProps.setSchemaVersion(JSON_SCHEMA_VERSION);

        return roboRunnerClient.updateWorker(new UpdateWorkerRequest()
                .withId(workerArn)
                .withAdditionalTransientProperties(OBJECT_MAPPER.writeValueAsString(addProps)));
    }

    //////////////////////////////////////////////////////////////////////////
    ///                          CHECK HELPERS                             ///
    //////////////////////////////////////////////////////////////////////////

    /**
     * A helper method that checks if the vendor worker is moving.
     *
     * @param vendorProperties the properties of the vendor worker
     * @return true if vendor worker is still executing, and false otherwise
     */
    private static boolean isVendorWorkerExecuting(@NonNull final VendorProperties vendorProperties) {
        try {
            final String vendorWorkerState = getVendorWorkerState(vendorProperties);
            return vendorWorkerState.equals(EXECUTING.value);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    ///                          VALIDATION HELPERS                        ///
    //////////////////////////////////////////////////////////////////////////

    /**
     * Validates that the worker updates happen continuously.
     *
     * @param durationsBetweenWorkerUpdates the list of durations between worker resource updates
     *                                      collected over a period of time
     */
    public static void validateDurationBetweenWorkerUpdatesIsInAllowedRange(
            @NonNull final List<Duration> durationsBetweenWorkerUpdates) {

        final int durationThreshold = integTestPropertyConfig.getWorkerUpdatePeriodInSeconds()
                + integTestPropertyConfig.getWorkerUpdateAllowedDelayInSeconds();

        durationsBetweenWorkerUpdates.stream()
                .map(Duration::getSeconds)
                .forEach(duration -> assertTrue(duration <= durationThreshold));
    }

    /**
     * Validates that the orientation of worker gets updated continuously.
     *
     * @param orientationChangeTimeSeries the list of orientation values collected over period of time
     * @param totalTravelledTimeInSeconds the time it took the vendor worker to perform a mission
     */
    public static void validateOrientationChangesBetweenWorkerUpdates(
            @NonNull final List<Double> orientationChangeTimeSeries,
            final long totalTravelledTimeInSeconds) {
        assertFalse(orientationChangeTimeSeries.isEmpty() || orientationChangeTimeSeries.size() == 1);

        final int maxTimeBetweenWorkerUpdates = integTestPropertyConfig.getWorkerUpdatePeriodInSeconds()
                + integTestPropertyConfig.getWorkerUpdateAllowedDelayInSeconds();

        // Since we cannot compare exact orientation values, we will need to decide how many times minimum we expect
        // the orientation value of vendor worker to update in IoT RoboRunner over the period of time that it moves.
        final long minRequiredOrientationUpdateCount = totalTravelledTimeInSeconds / maxTimeBetweenWorkerUpdates;

        final long orientationUpdateNum = IntStream.range(0, orientationChangeTimeSeries.size() - 1)
                .mapToObj(i ->
                        new double[]{orientationChangeTimeSeries.get(i), orientationChangeTimeSeries.get(i + 1)})
                .map(orientations ->
                        Math.abs(orientations[0] - orientations[1]) > DOUBLE_VALUE_COMPARISON_ALLOWED_ERROR)
                .count();

        assertTrue(orientationUpdateNum >= minRequiredOrientationUpdateCount);
    }

    /**
     * Validates that the orientation at the final destination of vendor worker is as expected and corresponds
     * to the final destination's orientation definition.
     *
     * @param vendorProperties the vendor properties of the worker
     * @param expectedOrientation the orientation value defined in the map
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static void validateFinalOrientationChangeAtDestination(
            @NonNull final VendorProperties vendorProperties,
            final Double expectedOrientation) throws JsonProcessingException {

        final double vendorOrientationInDegrees = getVendorWorkerOrientation(vendorProperties).getDegrees();

        assertEquals(expectedOrientation, vendorOrientationInDegrees,
                integTestPropertyConfig.getOrientationComparisonAllowedErrorInDegrees());
    }

    /**
     * Validates that the position properties of worker get updated continuously.
     *
     * @param currPosition the current position of the vendor worker in the prior calculated path
     * @param nextPosition the next position of the vendor worker in the prior calculated path
     * @param currState the state of vendor worker at the current position
     * @param nextState the state of vendor worker at the next position
     * @param prevTravelledDist the calculated distance between the previous position and current position
     * @return the distance between the current and next positions
     */
    public static double validateVendorWorkerMoveUpdatesInIotRoboRunner(
            @NonNull final PositionCoordinates currPosition,
            @NonNull final PositionCoordinates nextPosition,
            @NonNull final String currState,
            @NonNull final String nextState,
            final double prevTravelledDist) {
        final double dist = TestHelpers.getDistanceInCartesianCoordinateSystem(
                currPosition.getCartesianCoordinates().getX(),
                currPosition.getCartesianCoordinates().getY(),
                nextPosition.getCartesianCoordinates().getX(),
                nextPosition.getCartesianCoordinates().getY());

        if (currState.equals(EXECUTING.value) && nextState.equals(EXECUTING.value)) {
            assertTrue(prevTravelledDist + dist > prevTravelledDist);
        }

        return prevTravelledDist + dist;
    }

    /**
     * Validates that the vendor worker's position at the final destination is reflected in corresponding
     * IoT RoboRunner Worker's vendor properties.
     *
     * @param finalPosition the final position of the vendor worker where it stops
     * @param expectedFinalPosition the expected position of the vendor worker as defined in its map
     */
    public static void validateVendorWorkerFinalPositionInIotRoboRunner(
            @NonNull final PositionCoordinates finalPosition,
            @NonNull final PositionCoordinates expectedFinalPosition) {
        final double dist = TestHelpers.getDistanceInCartesianCoordinateSystem(
                expectedFinalPosition.getCartesianCoordinates().getX(),
                expectedFinalPosition.getCartesianCoordinates().getY(),
                finalPosition.getCartesianCoordinates().getX(),
                finalPosition.getCartesianCoordinates().getY());

        assertTrue(dist < integTestPropertyConfig.getCoordinatesComparisonAllowedErrorInMeters());
    }

    /**
     * Validates that the state of vendor worker gets updated in IoT RoboRunner continuously.
     *
     * @param stateUpdates list containing the state changes over the period of mission
     */
    public static void validateVendorWorkerStateUpdatesInIotRoboRunner(@NonNull final List<String> stateUpdates) {
        final List<String> deduplicatedStateList = TestHelpers.removeSequentialDuplicates(stateUpdates);

        assertTrue(deduplicatedStateList.size() == 3);
        assertTrue(deduplicatedStateList.equals(List.of(READY.value, EXECUTING.value, READY.value)));
    }
}
