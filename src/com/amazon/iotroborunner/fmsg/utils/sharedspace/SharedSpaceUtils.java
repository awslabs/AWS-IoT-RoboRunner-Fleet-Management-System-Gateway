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

package com.amazon.iotroborunner.fmsg.utils.sharedspace;

import com.amazon.iotroborunner.fmsg.types.callback.AccessSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.callback.FailureMessage;
import com.amazon.iotroborunner.fmsg.types.callback.ReleaseSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.sharedspace.DestinationAdditionalInformation;
import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.amazonaws.services.iotroborunner.model.Destination;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * A utility class that contains helpful functionality to manipulate and work with shared spaces.
 */
@Log4j2
public final class SharedSpaceUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    /**
     * Hidden Constructor.
     */
    private SharedSpaceUtils() {
        throw new UnsupportedOperationException("This class is for holding utilities and should not be instantiated.");
    }

    /**
     * Extracts the additionalInformation field from the given destination resource.
     *
     * @param destination destination to extract additionalInformation from
     * @return optional DestinationAdditionalInformation
     */
    private static Optional<DestinationAdditionalInformation> extractAdditionalInformation(
        @NonNull final Destination destination) {
        log.debug("Extracting the additional fixed properties for {}. \n Destination: {} \n",
            destination.getName(), destination);
        try {
            return Optional.of(OBJECT_MAPPER.readValue(
                destination.getAdditionalFixedProperties(),
                DestinationAdditionalInformation.class));
        } catch (final JsonProcessingException ex) {
            log.warn(String.format("Failed to read Destination additional information: %s", ex.getMessage()), ex);
            return Optional.empty();
        }
    }

    /**
     * Builds a shared space that represents the RoboRunner destination for the provided vendor.
     *
     * @param destination       destination resource that we're mirroring in the shared space
     * @param vendorSharedSpace vendor shared space the shared space must belong to
     * @return a completed shared space
     */
    public static SharedSpace createSharedSpace(@NonNull final Destination destination,
                                                @NonNull final VendorSharedSpace vendorSharedSpace) {

        if (StringUtils.isBlank(destination.getName()) || StringUtils.isBlank(destination.getSite())) {
            // PreCondition: Used to ensure that we are able to map the SharedSpace to the right name and site
            log.error("[Missing Field] Unable to create Shared Space because the provided destination has "
                + "no name or site. Destination: {}", destination.toString());
            throw new IllegalArgumentException("The destination's name and site properties are required");
        }

        if (StringUtils.isBlank(vendorSharedSpace.getWorkerFleet())) {
            // PreCondition: Used to match the SharedSpace with the provided vendor shared space details
            log.error("[Missing Field] Unable to create Shared Space due to missing worker fleet ARN in the "
                + "vendor shared space. VendorSharedSpace: {}", vendorSharedSpace.toString());
            throw new IllegalArgumentException("The vendorSharedSpace's workerFleetArn property is required");
        }

        return SharedSpace.builder()
            .withDestinationArn(destination.getArn())
            .withName(destination.getName())
            .withSiteArn(destination.getSite())
            .withState(destination.getState())
            .withVendorSharedSpace(vendorSharedSpace)
            .build();
    }

    /**
     * Create a filtered list of RoboRunner shared spaces for a specific vendor (via worker fleet arn)
     * which is particularly important because the destination resource is used to represent standard destinations and
     * RoboRunner shared spaces.
     *
     * @param destinations list of destinations associated with a site to filter
     * @param fleetArn     worker fleet arn use to filter for a specific vendor
     * @return filtered list of shared spaces
     */
    public static List<SharedSpace> extractSharedSpaces(
        @NonNull final List<Destination> destinations,
        @NonNull final String fleetArn) {

        if (StringUtils.isBlank(fleetArn)) {
            throw new IllegalArgumentException("The fleetArn cannot be null/empty when extracting shared spaces");
        }

        final List<SharedSpace> sharedSpaces = new ArrayList<>();
        for (final Destination destination : destinations) {
            final Optional<DestinationAdditionalInformation> destinationInfo =
                extractAdditionalInformation(destination);
            if (destinationInfo.isPresent()) {
                // Shared Spaces modelled via the Destination Resource must have a populated additionalInformation field
                final List<VendorSharedSpace> vendorSharedSpaces = destinationInfo.get().getVendorSharedSpaces();
                if (vendorSharedSpaces != null && vendorSharedSpaces.size() > 0) {
                    // If the vendorShareSpaces property exists within the additional information with at least 1
                    // non-empty object, then we will consider the destination to be a shared space.
                    for (VendorSharedSpace vendorSharedSpace : vendorSharedSpaces) {
                        if (fleetArn.equals(vendorSharedSpace.getWorkerFleet())) {
                            final SharedSpace sharedSpace =
                                SharedSpaceUtils.createSharedSpace(destination, vendorSharedSpace);
                            sharedSpaces.add(sharedSpace);
                            break;
                        }
                    }
                }
            }
        }
        return sharedSpaces;
    }

    /**
     * Gathers the arns of all the RoboRunner shared spaces that are linked to the site.
     *
     * @param siteDestinations list of all destinations linked to a site
     * @return list of RoboRunner shared space arns
     */
    public static List<String> getRoboRunnerSharedSpaceArns(@NonNull final List<Destination> siteDestinations) {
        return siteDestinations.stream()
            .filter(destination -> extractAdditionalInformation(destination).isPresent())
            .map(Destination::getArn)
            .collect(Collectors.toList());
    }

    /**
     * Verifies that the provided worker holds a lock for the given shared space.
     *
     * @param workerArn          arn of the worker to verify
     * @param sharedSpaceArn     arn of the shared space we're checking
     * @param optionalLockHolder optional holding the lock holder record
     * @return true if the worker has a lock for the shared space else false
     */
    public static Optional<PriorityQueueRecord> verifyWorkerHoldsLockForSharedSpace(
        @NonNull final String workerArn,
        @NonNull final String sharedSpaceArn,
        @NonNull final Optional<PriorityQueueRecord> optionalLockHolder) {

        final AtomicBoolean isVerifiedLockHolder = new AtomicBoolean(true);
        optionalLockHolder.ifPresentOrElse(
            lockHolder -> {
                if (!workerArn.equals(lockHolder.getWorkerArn())) {
                    log.error("[PHYSICAL MISMATCH] No actions can be taken because workerArn: {} doesn't "
                            + "hold the lock for sharedSpaceArn: {}. The worker who actually holds the lock is: {}",
                        workerArn,
                        sharedSpaceArn,
                        lockHolder.getWorkerArn());
                    isVerifiedLockHolder.set(false);
                }
            }, () -> {
                log.error("[PHYSICAL MISMATCH]: WorkerArn: {} doesn't have a lock for sharedSpaceArn: {} "
                    + "because no worker does.", workerArn, sharedSpaceArn);
                isVerifiedLockHolder.set(false);
            });
        return isVerifiedLockHolder.get() ? optionalLockHolder : Optional.empty();
    }

    /**
     * Converts the provided JSON String into a AccessSharedSpaceRequest.
     *
     * @param response response to convert
     * @return optional AccessSharedSpaceRequest
     */
    public static Optional<AccessSharedSpaceRequest> convertToAccessSharedSpaceRequest(@NonNull final String response) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(response, AccessSharedSpaceRequest.class));
        } catch (final JsonProcessingException e) {
            log.error("Unable to process the provided Access Shared Space callback response: {} because {}",
                response,
                e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Converts the provided JSON String into a ReleaseSharedSpaceRequest.
     *
     * @param response response to convert
     * @return optional ReleaseSharedSpaceRequest
     */
    public static Optional<ReleaseSharedSpaceRequest> convertToReleaseSharedSpaceRequest(
        @NonNull final String response) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(response, ReleaseSharedSpaceRequest.class));
        } catch (final JsonProcessingException e) {
            log.error("Unable to process the provided Release Shared Space callback response: {} because {}",
                response,
                e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Converts the provided JSON String into a FailureMessage.
     *
     * @param response response to convert
     * @return optional FailureMessage
     */
    public static Optional<FailureMessage> convertToFailureMessage(
        @NonNull final String response) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(response, FailureMessage.class));
        } catch (final JsonProcessingException e) {
            log.error("Unable to process the provided Failure Message callback response: {} because {}",
                response,
                e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Take the shared space position coordinates and creates a polygon
     * object representing the shared space's shape.
     *
     * @param coordinates   list of coordinates from the shared space position values
     * @return              optional of polygon object representing the shared space shape
     */
    public static Optional<Polygon> createSharedSpacePolygon(@NonNull final List<Point> coordinates) {
        if (coordinates.isEmpty()) {
            return Optional.empty();
        }
        final List<Coordinate> coords = new ArrayList<Coordinate>();

        for (final Point point : coordinates) {
            coords.add(new Coordinate(point.getX(), point.getY()));
        }

        final List<Coordinate> closedCoords = closeCoordinateCircle(coords);

        final LinearRing ring = GEOMETRY_FACTORY.createLinearRing(closedCoords.toArray(new Coordinate[0]));
        final LinearRing[] holes = null;
        final Polygon polygon = GEOMETRY_FACTORY.createPolygon(ring, holes);

        return Optional.of(polygon);
    }

    /**
     * Checks if a list of coordinates have the same start and end coordinates
     * and if not adds the same start coordinate to the end of the list. It's purpose is to close the
     * coordinates to create a LinearRing object used to create the shared space polygon.
     *
     * @param inputCoords   list of coordinates from the shared space position values
     * @return              valid list of coordinates for LinearRing object
     */
    public static List<Coordinate> closeCoordinateCircle(@NonNull final List<Coordinate> inputCoords) {
        if (inputCoords.isEmpty()) {
            return inputCoords;
        }
        if (Double.compare(inputCoords.get(0).getX(), inputCoords.get(inputCoords.size() - 1).getX()) != 0
                 || Double.compare(inputCoords.get(0).getY(), inputCoords.get(inputCoords.size() - 1).getY()) != 0) {
            inputCoords.add(inputCoords.get(0));
        }
        return inputCoords;
    }

    /**
     * Determine if a robot is within 2 meters of a vendor shared space.
     * The purpose is to notify that a robot is requesting the shared space.
     *
     * @param robotPosition     robot position point to calculate distance between
     * @param polygon           shared space polygon to calculate distance between
     * @return                  boolean is true if within two meters of vendor shared space
     */
    public static Boolean robotIsWithinTwoMeters(@NonNull final Point robotPosition,
                                                           @NonNull final Polygon polygon) {
        log.debug("Robot position: {}, Polygon: {}", robotPosition, polygon);
        if (robotPosition.within(polygon)) {
            log.info("Robot isn't within 2 meters because it's already within the shared space.");
            return false;
        }
        return Double.compare(robotPosition.distance(polygon), 2.0) <= 0;

    }

    /**
     * Builds the request object that can be used to request access to a shared space.
     *
     * @param workerFleetArn the unique identifier of the worker fleet
     * @param workerArn the unique identifier of the worker
     * @param sharedSpaceArn the unique identifier of the shared space
     * @return the ready to use access request object
     */
    public static AccessSharedSpaceRequest buildAccessSharedSpaceRequest(@NonNull final String workerFleetArn,
                                                                         @NonNull final String workerArn,
                                                                         @NonNull final String sharedSpaceArn) {
        return AccessSharedSpaceRequest.builder()
                .workerFleetArn(workerFleetArn)
                .workerArn(workerArn)
                .sharedSpaceArn(sharedSpaceArn)
                .requestTime(Instant.now().toEpochMilli())
                .build();
    }

    /**
     * Builds the request object that can be used to notify about the need to release the lock to the shared space.
     *
     * @param workerFleetArn the unique identifier of the worker fleet
     * @param workerArn the unique identifier of the worker
     * @param sharedSpaceArn the unique identifier of the shared space
     * @return  the ready to use release request object
     */
    public static ReleaseSharedSpaceRequest buildReleaseSharedSpaceRequest(@NonNull final String workerFleetArn,
                                                                           @NonNull final String workerArn,
                                                                           @NonNull final String sharedSpaceArn) {
        return ReleaseSharedSpaceRequest.builder()
                .workerFleetArn(workerFleetArn)
                .workerArn(workerArn)
                .sharedSpaceArn(sharedSpaceArn)
                .releaseTime(Instant.now().toEpochMilli())
                .build();
    }
}
