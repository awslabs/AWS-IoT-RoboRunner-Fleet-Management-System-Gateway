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
import static com.amazon.iotroborunner.fmsg.testutils.dispatchers.MirCommandDispatcher.scheduleMirMission;
import static com.amazon.iotroborunner.fmsg.testutils.helpers.TestHelpers.getDistanceInCartesianCoordinateSystem;

import com.amazon.iotroborunner.fmsg.config.IntegrationTestPropertyConfiguration;
import com.amazon.iotroborunner.fmsg.testutils.dispatchers.MirCommandDispatcher;
import com.amazon.iotroborunner.fmsg.testutils.providers.ConfigurationProvider;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.types.mir.MirRobotStatus;
import com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.amazonaws.services.iotroborunner.model.GetWorkerResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import mapresources.MirMapPosition;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Collection of helper methods to assist with integration test logic specifically for MiR tests.
 */
public final class MirTestHelpers {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private static final int VENDOR_WORKER_INFO_POLLING_TIMEOUT_IN_MILLISECONDS = 1000;
    private static final double DOUBLE_COMPARISON_PRECISION = 0.000001d;

    private static final IntegrationTestPropertyConfiguration integTestPropertyConfig =
            ConfigurationProvider.getIntegrationTestPropertyConfigs().get(RobotFleetType.MIR.value);
    private static Map<String, String> mirMissionIds = integTestPropertyConfig.getTestMissionIds();

    /**
     * Schedules given MiR move mission for given vendor worker and waits until the vendor worker completes the mission
     * and reaches its target. While the vendor worker moves in FMS, this method also offers an option to return a
     * series of corresponding worker resources from IoT RoboRunner collected over the period of the mission execution.
     * The worker data is gatherer with 1 second frequency.
     *
     * @param missionName the unique identifier of a mission for a specific move
     * @param vendorWorkerIdToWorkerArnMapping the mapping between MiR vendor worker
     *                                  id and corresponding IoT RoboRunner worker arn
     * @param endDestination the end destination of the vendor worker
     * @param shouldCollectWorkerStatusUpdateHistory the boolean indicating whether the worker
     *                                               resource changes should be recorded and returned
     * @return if shouldCollectWorkerStatusUpdateHistory is enabled, returns a map of
     *         updateAt->worker updates mapping collected while mission was executing, otherwise empty map
     * @throws InterruptedException if Thread.sleep() is interrupted
     */
    public static NavigableMap<String, GetWorkerResult> executeMoveMissionUntilCompleteBasedOnIotRoboRunnerWorkerState(
            @NonNull final String missionName,
            @NonNull final Pair<Integer, String> vendorWorkerIdToWorkerArnMapping,
            @NonNull final MirMapPosition endDestination,
            final boolean shouldCollectWorkerStatusUpdateHistory)
            throws InterruptedException {

        final int vendorWorkerId = vendorWorkerIdToWorkerArnMapping.getKey();
        final String workerArn = vendorWorkerIdToWorkerArnMapping.getValue();
        final NavigableMap<String, GetWorkerResult> workerUpdateHistory = new TreeMap<>();

        // If collecting worker updates data, save the worker's initial state from IoT RoboRunner.
        if (shouldCollectWorkerStatusUpdateHistory) {
            final GetWorkerResult workerInfo = IotRoboRunnerTestHelpers.getWorkerById(workerArn);
            workerUpdateHistory.put(workerInfo.getUpdatedAt().toString(), workerInfo);
        }

        scheduleMirMission(mirMissionIds.get(missionName), vendorWorkerId, missionName);

        boolean arrived = false;
        MirRobotStatus vendorWorkerInfo;

        do {
            vendorWorkerInfo = MirCommandDispatcher.getVendorWorkerStatusById(vendorWorkerId);
            if (vendorWorkerInfo == null) {
                continue;
            }

            if (shouldCollectWorkerStatusUpdateHistory && vendorWorkerInfo.getState().equals(EXECUTING.value)) {
                final GetWorkerResult workerInfo = IotRoboRunnerTestHelpers.getWorkerById(workerArn);

                workerUpdateHistory.put(workerInfo.getUpdatedAt().toString(), workerInfo);
            }

            arrived = hasVendorWorkerArrived(vendorWorkerInfo.getRobotX(), vendorWorkerInfo.getRobotY(),
                    endDestination.positionX, endDestination.positionY);

            Thread.sleep(VENDOR_WORKER_INFO_POLLING_TIMEOUT_IN_MILLISECONDS);
        } while (!arrived || !isVendorWorkerResetAndReady(vendorWorkerInfo.getState()));

        // To make sure the very last update, when the vendor worker is reset and free, made it to
        // IoT RoboRunner, we will need to wait the maximum allowed time it needs to be synced.
        Thread.sleep((integTestPropertyConfig.getWorkerUpdatePeriodInSeconds()
                + integTestPropertyConfig.getWorkerUpdateAllowedDelayInSeconds()) * 1000);

        // If collecting worker updates data, save the worker's final state from
        // IoT RoboRunner after the mission is finished
        if (shouldCollectWorkerStatusUpdateHistory) {
            final GetWorkerResult workerInfo = IotRoboRunnerTestHelpers.getWorkerById(workerArn);

            workerUpdateHistory.put(workerInfo.getUpdatedAt().toString(), workerInfo);
        }

        return workerUpdateHistory;
    }

    /**
     * Schedules given MiR move mission for given vendor worker and waits until the vendor worker completes the mission
     * and reaches its target.
     *
     * @param missionName the unique identifier of a mission for a specific move
     * @param vendorWorkerId the unique identifier of the vendor worker
     * @param endDestination the end destination of the vendor worker
     * @throws InterruptedException if Thread.sleep() is interrupted
     */
    public static void executeMoveMissionUntilCompleteBasedOnVendorWorkerStatus(
            @NonNull final String missionName,
            final int vendorWorkerId,
            @NonNull final MirMapPosition endDestination) throws InterruptedException {
        MirRobotStatus currentVendorWorkerStatus;

        scheduleMirMission(mirMissionIds.get(missionName), vendorWorkerId, missionName);

        boolean arrived = false;

        do {
            currentVendorWorkerStatus = MirCommandDispatcher.getVendorWorkerStatusById(vendorWorkerId);
            if (currentVendorWorkerStatus == null) {
                continue;
            }

            arrived = hasVendorWorkerArrived(currentVendorWorkerStatus.getRobotX(),
                    currentVendorWorkerStatus.getRobotY(), endDestination.positionX, endDestination.positionY);

            Thread.sleep(VENDOR_WORKER_INFO_POLLING_TIMEOUT_IN_MILLISECONDS);
        } while (!arrived || !isVendorWorkerResetAndReady(currentVendorWorkerStatus.getState()));
    }

    /**
     * Checks whether the vendor worker reached its final destination.
     *
     * @param vendorWorkerX the x coordinate of the vendor worker
     * @param vendorWorkerY the y coordinate of the vendor worker
     * @param destinationX the x coordinate of the end destination of the vendor worker
     * @param destinationY the y coordinate of the end destination of the vendor worker
     * @return true if the vendor worker arrived to its destination, and false otherwise
     */
    private static boolean hasVendorWorkerArrived(final double vendorWorkerX, final double vendorWorkerY,
                                                  final double destinationX, final double destinationY) {
        final double distance = getDistanceInCartesianCoordinateSystem(
                vendorWorkerX, vendorWorkerY, destinationX, destinationY);
        return distance < integTestPropertyConfig.getCoordinatesComparisonAllowedErrorInMeters();
    }

    /**
     * Constructs a polygon to represent the shared space geometrically.
     *
     * @param sharedSpaceId the unique vendor shared space identifier
     * @return a polygon representing the shared space
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static Polygon getSharedSpacePolygon(@NonNull final String sharedSpaceId) throws JsonProcessingException {
        final String sharedSpaceStr = MirCommandDispatcher.getSharedSpace(sharedSpaceId);
        final JsonNode polygonMetaData = OBJECT_MAPPER.readTree(sharedSpaceStr).get("polygon");

        List<Coordinate> points = new ArrayList<>();
        for (JsonNode polygonMetaDatum : polygonMetaData) {
            final double xCoord = polygonMetaDatum.get("x").asDouble();
            final double yCoord = polygonMetaDatum.get("y").asDouble();
            points.add(new Coordinate(xCoord, yCoord));
        }
        points = SharedSpaceUtils.closeCoordinateCircle(points);

        final Coordinate[] coordinateArr = points.toArray(new Coordinate[points.size()]);

        final LinearRing linearRing = GEOMETRY_FACTORY.createLinearRing(coordinateArr);
        return GEOMETRY_FACTORY.createPolygon(linearRing);

    }

    /**
     * Checks if provided coordinate is inside the polygon representing a shared space.
     *
     * @param coordinate the coordinate to be checked
     * @param sharedSpaceId the vendor unique identifier of a shared space
     * @return true if the coordinate is inside the polygon, and false otherwise
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static boolean isCoordinateInSharedSpace(
            @NonNull final String sharedSpaceId,
            @NonNull final Coordinate coordinate) throws JsonProcessingException {
        final Polygon sharedSpacePolygon = getSharedSpacePolygon(sharedSpaceId);

        return sharedSpacePolygon.contains(GEOMETRY_FACTORY.createPoint(coordinate));
    }

    /**
     * Checks if the vendor worker crossed the shared space or is on the left from it.
     *
     * @param sharedSpaceId the unique identifier of the shared space
     * @param vendorWorkerPositionCoordX the x coordinate of the vendor worker's location
     * @return true if the worker is not on the left from the most left point of
     *         the shared space, and false otherwise
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static boolean didVendorWorkerEnterSharedSpace(
            @NonNull final String sharedSpaceId,
            final double vendorWorkerPositionCoordX)
            throws JsonProcessingException {
        final double sharedSpaceLeftMostCoordX = getSharedSpaceMinX(sharedSpaceId);
        return sharedSpaceLeftMostCoordX - vendorWorkerPositionCoordX > 0;
    }

    /**
     * Finds the left most X coordinate of the shared space.
     *
     * @param sharedSpaceId the unique identifier of a shared space
     * @return the left most X coordinate of the given shared space
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    private static Double getSharedSpaceMinX(@NonNull final String sharedSpaceId) throws JsonProcessingException {
        final Polygon sharedSpacePolygon = getSharedSpacePolygon(sharedSpaceId);

        double minX = Double.MAX_VALUE;
        for (Coordinate coordinate : sharedSpacePolygon.getCoordinates()) {
            if (Double.compare(minX, coordinate.getX()) > 0) {
                minX = coordinate.getX();
            }
        }

        return minX;
    }

    /**
     * Checks if the vendor worker is at given position.
     *
     * @param vendorWorkerPosition the position of the vendor worker
     * @param expectedPosition the expected position of the vendor worker
     * @return true if the vendor worker is at the given position, and false otherwise
     */
    public static boolean isVendorWorkerAtGivenPosition(
            @NonNull final Coordinate vendorWorkerPosition,
            @NonNull final Coordinate expectedPosition) {
        final double dist = TestHelpers.getDistanceInCartesianCoordinateSystem(
                expectedPosition.getX(),
                expectedPosition.getY(),
                vendorWorkerPosition.getX(),
                vendorWorkerPosition.getY());

        return dist < integTestPropertyConfig.getCoordinatesComparisonAllowedErrorInMeters();
    }

    /**
     * Private helper that checks if vendor worker finished its mission and is reset to be ready for further missions.
     *
     * @param vendorWorkerState the current state of the vendor worker
     * @return true if the vendor worker is in state of readiness for new missions, and false otherwise
     */
    private static boolean isVendorWorkerResetAndReady(@NonNull final String vendorWorkerState) {
        return vendorWorkerState.equals(READY.value);
    }

    /**
     * Checks if the vendor worker moved since the last check (based on X, Y coordinate changes).
     *
     * @param prevStatus the previous state of the vendor worker
     * @param currStatus the current state of the vendor worker
     * @return true if the vendor worker moved, and false otherwise
     */
    public static boolean hasVendorWorkerMovedSinceLastCheck(@NonNull final MirRobotStatus prevStatus,
                                                             @NonNull final MirRobotStatus currStatus) {

        return Math.abs(prevStatus.getRobotX() - currStatus.getRobotX()) > DOUBLE_COMPARISON_PRECISION
                || Math.abs(prevStatus.getRobotY() - currStatus.getRobotY()) > DOUBLE_COMPARISON_PRECISION;
    }
}
