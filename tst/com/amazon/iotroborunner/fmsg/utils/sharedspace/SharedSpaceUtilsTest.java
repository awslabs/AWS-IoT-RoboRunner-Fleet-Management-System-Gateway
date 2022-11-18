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

import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.FAILURE_MESSAGE;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.TestConstants.WORKER_FLEET_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.SHARED_SPACE_DESTINATION_ADDITIONAL_INFO;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.SHARED_SPACE_DESTINATION_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.STANDARD_DESTINATION_ADDITIONAL_INFO;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.DestinationTestConstants.STANDARD_DESTINATION_ARN;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createDestinationAdditionalInformationTestResource;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createDestinationTestResource;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createVendorSharedSpaceTestResource;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.convertToAccessSharedSpaceRequest;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.convertToFailureMessage;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.convertToReleaseSharedSpaceRequest;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.getRoboRunnerSharedSpaceArns;
import static com.amazon.iotroborunner.fmsg.utils.sharedspace.SharedSpaceUtils.verifyWorkerHoldsLockForSharedSpace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.testhelpers.MockedAppender;
import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.testhelpers.TestUtils;
import com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestConstants;
import com.amazon.iotroborunner.fmsg.types.callback.AccessSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.callback.FailureMessage;
import com.amazon.iotroborunner.fmsg.types.callback.ReleaseSharedSpaceRequest;
import com.amazon.iotroborunner.fmsg.types.sharedspace.DestinationAdditionalInformation;
import com.amazon.iotroborunner.fmsg.types.sharedspace.PriorityQueueRecord;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.DestinationState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the Shared Space utility module.
 */
@ExtendWith(MockitoExtension.class)
public class SharedSpaceUtilsTest {
    @Mock
    private Destination mockDestination;

    private static Logger logger;
    private static MockedAppender mockedAppender;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    /**
     * Set up the mocks needed for each subsequent test.
     */
    @BeforeAll
    public static void setUp() {
        mockedAppender = new MockedAppender();
        logger = (Logger) LogManager.getLogger(SharedSpaceUtils.class);
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.DEBUG);
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

    @Test
    public void given_destinationWithNoName_when_createSharedSpace_then_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            SharedSpaceUtils.createSharedSpace(
                new Destination(), new VendorSharedSpace(WORKER_FLEET_ARN));
        });
    }

    @Test
    public void given_destinationWithNoSiteArn_when_createSharedSpace_then_throwsIllegalArgumentException() {
        // Given
        final Destination destination = createDestinationTestResource("TestDestination", null,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED, "AdditionalInfoStr");

        // Testing and Verification
        assertThrows(IllegalArgumentException.class, () -> {
            SharedSpaceUtils.createSharedSpace(destination, new VendorSharedSpace(WORKER_FLEET_ARN));
        });
    }

    @ParameterizedTest
    @EmptySource
    void given_vendorSharedSpaceWithoutFleet_when_createSharedSpace_then_throwsIllegalArgumentException(
        final String emptyFleetArn) {

        final Destination destination = createDestinationTestResource("TestDestination", null,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED, "AdditionalInfoStr");
        final VendorSharedSpace vendorSharedSpaceMissingArn = createVendorSharedSpaceTestResource(
            emptyFleetArn, TestUtils.generateId());

        assertThrows(IllegalArgumentException.class, () -> {
            SharedSpaceUtils.createSharedSpace(destination, vendorSharedSpaceMissingArn);
        });

    }

    @Test
    public void given_destination_when_createSharedSpace_then_returnsSharedSpace() {
        // Given
        final String destinationName = "TestDestination";
        final Destination destination = createDestinationTestResource(destinationName, TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED, "AdditionalInfoStr");

        final String vendorSharedSpaceGuid = TestUtils.generateId();
        final VendorSharedSpace vendorSharedSpace = createVendorSharedSpaceTestResource(
            WORKER_FLEET_ARN, vendorSharedSpaceGuid);

        // Testing
        final SharedSpace sharedSpaceResult = SharedSpaceUtils.createSharedSpace(destination, vendorSharedSpace);

        // Verification
        assertNotNull(sharedSpaceResult);
        assertEquals(destinationName, sharedSpaceResult.getName());
        assertEquals(TestConstants.SITE_ARN, sharedSpaceResult.getSiteArn());
        assertEquals(DestinationState.ENABLED.toString(), sharedSpaceResult.getState());

        assertNotNull(sharedSpaceResult.getVendorSharedSpace());
        assertEquals(WORKER_FLEET_ARN, sharedSpaceResult.getVendorSharedSpace().getWorkerFleet());
        assertEquals(vendorSharedSpaceGuid, sharedSpaceResult.getVendorSharedSpace().getGuid());
    }

    @Test
    public void given_noDestinations_when_extractSharedSpaces_then_returnsEmptyList() throws JsonProcessingException {
        // Given
        final List<Destination> emptyList = new ArrayList<>();

        // Testing
        final List<SharedSpace> sharedSpacesResult = SharedSpaceUtils.extractSharedSpaces(
            emptyList, WORKER_FLEET_ARN);

        // Verification
        assertEquals(0, sharedSpacesResult.size());
    }

    @ParameterizedTest
    @EmptySource
    void given_EmptyFleetArn_when_extractSharedSpaces_then_throwsIllegalArgumentException(
        final String fleetArn) {
        assertThrows(IllegalArgumentException.class, () -> {
            SharedSpaceUtils.extractSharedSpaces(List.of(mockDestination), fleetArn);
        });
    }

    @Test
    public void given_listOfDestinationsAndFleetArn_when_extractSharedSpaces_then_returnsListOfSharedSpacesForFleet()
            throws JsonProcessingException {
        // Given

        final VendorSharedSpace vendor1SharedSpace = createVendorSharedSpaceTestResource(
            WORKER_FLEET_ARN, TestUtils.generateId());

        final VendorSharedSpace vendor2SharedSpace = createVendorSharedSpaceTestResource(
            TestUtils.generateSimilarArn(WORKER_FLEET_ARN), null);

        final DestinationAdditionalInformation destinationAdditionalInformation =
            createDestinationAdditionalInformationTestResource(Stream.of(vendor1SharedSpace, vendor2SharedSpace)
                .collect(Collectors.toList()));

        final String additionalInformationStr = OBJECT_MAPPER.writeValueAsString(destinationAdditionalInformation);

        final Destination localDestination1 = createDestinationTestResource("Destination1", TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN, DestinationState.ENABLED, additionalInformationStr);
        final Destination localDestination2 = createDestinationTestResource("Destination2", TestConstants.SITE_ARN,
            TestUtils.generateSimilarArn(TestConstants.DESTINATION_ARN), DestinationState.ENABLED,
            additionalInformationStr);

        final List<Destination> destinations =
            Stream.of(localDestination1, localDestination2).collect(Collectors.toList());

        // Testing
        final List<SharedSpace> sharedSpacesResult = SharedSpaceUtils.extractSharedSpaces(
            destinations, WORKER_FLEET_ARN);

        // Verification
        assertNotNull(sharedSpacesResult);
        assertEquals(2, sharedSpacesResult.size());
        sharedSpacesResult.forEach(Assertions::assertNotNull);

        // Since only differences between setup destinations are the name and arn,
        // checking if the names/ARNs match between the destination and according
        // SharedSpace object should suffice.
        assertEquals(localDestination1.getName(), sharedSpacesResult.get(0).getName());
        assertEquals(localDestination1.getArn(), sharedSpacesResult.get(0).getDestinationArn());

        assertEquals(localDestination2.getName(), sharedSpacesResult.get(1).getName());
        assertEquals(localDestination2.getArn(), sharedSpacesResult.get(1).getDestinationArn());
    }

    @Test
    public void given_emptyDestinations_when_getRoboRunnerSharedSpaceArns_then_returnEmptyList() {
        final List<Destination> emptyDestinationsList = Collections.emptyList();

        final List<String> result = getRoboRunnerSharedSpaceArns(emptyDestinationsList);

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_nonStandardSiteDestination_when_getRoboRunnerSharedSpaceArns_then_returnEmptyList() {
        final Destination standardDestination = createDestinationTestResource("TestStandardDestination",
            TestConstants.SITE_ARN,
            TestConstants.DESTINATION_ARN,
            DestinationState.ENABLED,
            STANDARD_DESTINATION_ADDITIONAL_INFO);

        final List<String> result = getRoboRunnerSharedSpaceArns(Stream.of(standardDestination)
            .collect(Collectors.toList()));

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_mixedSiteDestinations_when_getRoboRunnerSharedSpaceArns_then_returnOnlySharedSpaces()
            throws JsonProcessingException {
        final Destination standardDestination = createDestinationTestResource("TestStandardDestination",
            TestConstants.SITE_ARN,
            STANDARD_DESTINATION_ARN,
            DestinationState.ENABLED,
            STANDARD_DESTINATION_ADDITIONAL_INFO);
        final Destination sharedSpaceDestination = createDestinationTestResource("TestSharedSpaceDestination",
            TestConstants.SITE_ARN,
            SHARED_SPACE_DESTINATION_ARN,
            DestinationState.ENABLED,
            OBJECT_MAPPER.writeValueAsString(SHARED_SPACE_DESTINATION_ADDITIONAL_INFO));

        final List<String> result = getRoboRunnerSharedSpaceArns(Stream.of(standardDestination, sharedSpaceDestination)
            .collect(Collectors.toList()));

        assertEquals(1, result.size());
        assertEquals(sharedSpaceDestination.getArn(), result.get(0));
    }

    @Test
    void given_validSharedSpaceCoordinates_when_createSharedSpacePolygon_then_returnValidPolygon() {
        final Polygon polygon =
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_1).get();

        final Coordinate[] coords = polygon.getCoordinates();
        int i = 0;
        for (Coordinate coord : coords) {
            // LinearRing object requires that the coordinate list begin and end with the same coordinates
            if (i > SharedSpaceTestConstants.POLYGON_COORDS_1.size() - 1) {
                i = 0;
            }
            assertEquals(SharedSpaceTestConstants.POLYGON_COORDS_1.get(i).getX(), coord.getX());
            assertEquals(SharedSpaceTestConstants.POLYGON_COORDS_1.get(i).getY(), coord.getY());
            i++;
        }
    }

    @Test
    void given_startAndEndSameCoordinates_when_createSharedSpacePolygon_then_returnValidPolygon() {
        final Polygon polygon =
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_SAME).get();

        final Coordinate[] coords = polygon.getCoordinates();
        int i = 0;
        for (Coordinate coord : coords) {
            assertEquals(SharedSpaceTestConstants.POLYGON_COORDS_SAME.get(i).getX(), coord.getX());
            assertEquals(SharedSpaceTestConstants.POLYGON_COORDS_SAME.get(i).getY(), coord.getY());
            i++;
        }
    }

    @Test
    void given_emptySharedSpaceCoordinates_when_createSharedSpacePolygon_then_returnEmptyOptional() {
        assertTrue(SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_EMPTY).isEmpty());
    }

    @Test
    void given_singleSharedSpaceCoordinate_when_createSharedSpacePolygon_then_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_SINGLE);
        });
    }

    @Test
    void given_twoSameSharedSpaceCoordinates_when_createSharedSpacePolygon_then_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_TWO_SAME);
        });
    }

    @Test
    void given_twoDiffSharedSpaceCoordinates_when_createSharedSpacePolygon_then_throwsIllegalArgument() {
        final Polygon polygon =
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_TWO_DIFF).get();

        final Coordinate[] coords = polygon.getCoordinates();
        int i = 0;
        for (final Coordinate coord : coords) {
            // LinearRing object requires that the coordinate list begin and end with the same coordinates
            if (i > SharedSpaceTestConstants.POLYGON_COORDS_TWO_DIFF.size() - 1) {
                i = 0;
            }
            assertEquals(SharedSpaceTestConstants.POLYGON_COORDS_TWO_DIFF.get(i).getX(), coord.getX());
            assertEquals(SharedSpaceTestConstants.POLYGON_COORDS_TWO_DIFF.get(i).getY(), coord.getY());
            i++;
        }
    }

    @Test
    void given_validClosedCoordinates_when_closeCoordinateCircle_then_returnSameClosedCoordinates() {
        final List<Coordinate> coords = new ArrayList<Coordinate>();
        for (final Point point : SharedSpaceTestConstants.POLYGON_COORDS_SAME) {
            coords.add(new Coordinate(point.getX(), point.getY()));
        }

        assertEquals(coords, SharedSpaceUtils.closeCoordinateCircle(coords));
    }

    @Test
    void given_notClosedCoordinates_when_closeCoordinateCircle_then_returnClosedCoordinates() {
        final List<Coordinate> notClosedCoords = new ArrayList<Coordinate>();
        for (final Point point : SharedSpaceTestConstants.POLYGON_COORDS_1) {
            notClosedCoords.add(new Coordinate(point.getX(), point.getY()));
        }
        final List<Coordinate> closedCoords = new ArrayList<Coordinate>();
        for (final Point point : SharedSpaceTestConstants.POLYGON_COORDS_SAME) {
            closedCoords.add(new Coordinate(point.getX(), point.getY()));
        }

        assertEquals(closedCoords, SharedSpaceUtils.closeCoordinateCircle(notClosedCoords));
    }

    @Test
    void given_emptyCoordinates_when_closeCoordinateCircle_then_returnEmptyCoordinates() {
        final List<Coordinate> coords = new ArrayList<Coordinate>();

        assertEquals(coords, SharedSpaceUtils.closeCoordinateCircle(coords));
    }

    @Test
    void given_singleCoordinates_when_closeCoordinateCircle_then_returnSingleCoordinate() {
        final List<Coordinate> notClosedCoords = new ArrayList<Coordinate>();
        for (final Point point : SharedSpaceTestConstants.POLYGON_COORDS_SINGLE) {
            notClosedCoords.add(new Coordinate(point.getX(), point.getY()));
        }

        assertEquals(notClosedCoords, SharedSpaceUtils.closeCoordinateCircle(notClosedCoords));
    }

    @Test
    public void given_validResponse_when_convertToAccessSharedSpaceRequest_then_returnPresentOptional()
            throws JsonProcessingException {
        final String validResponse = OBJECT_MAPPER.writeValueAsString(
            AccessSharedSpaceRequest.builder()
                .workerArn(WORKER_ARN)
                .workerFleetArn(WORKER_FLEET_ARN)
                .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
                .build());

        final Optional<AccessSharedSpaceRequest> result = convertToAccessSharedSpaceRequest(validResponse);

        assertTrue(result.isPresent());
    }

    @Test
    public void given_invalidResponse_when_convertToAccessSharedSpaceRequest_then_returnEmptyOptional() {
        final String invalidResponse = "INVALID_RESPONSE";

        final Optional<AccessSharedSpaceRequest> result = convertToAccessSharedSpaceRequest(invalidResponse);

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_validResponse_when_convertToReleaseSharedSpaceRequest_then_returnPresentOptional()
            throws JsonProcessingException {
        final String validResponse = OBJECT_MAPPER.writeValueAsString(
            ReleaseSharedSpaceRequest.builder()
                .workerArn(WORKER_ARN)
                .workerFleetArn(WORKER_FLEET_ARN)
                .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
                .build());

        final Optional<ReleaseSharedSpaceRequest> result = convertToReleaseSharedSpaceRequest(validResponse);

        assertTrue(result.isPresent());
    }

    @Test
    public void given_invalidResponse_when_convertToReleaseSharedSpaceRequest_then_returnEmptyOptional() {
        final String invalidResponse = "INVALID_RESPONSE";

        final Optional<ReleaseSharedSpaceRequest> result = convertToReleaseSharedSpaceRequest(invalidResponse);

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_validResponse_when_convertToFailureMessage_then_returnPresentOptional()
            throws JsonProcessingException {
        final String validResponse = OBJECT_MAPPER.writeValueAsString(
            FailureMessage.builder()
                .workerArn(WORKER_ARN)
                .workerFleetArn(WORKER_FLEET_ARN)
                .sharedSpaceArn(SHARED_SPACE_DESTINATION_ARN)
                .message(FAILURE_MESSAGE)
                .build());

        final Optional<FailureMessage> result = convertToFailureMessage(validResponse);

        assertTrue(result.isPresent());
    }

    @Test
    public void given_invalidResponse_when_convertToFailureMessage_then_returnEmptyOptional() {
        final String invalidResponse = "INVALID_RESPONSE";

        final Optional<FailureMessage> result = convertToFailureMessage(invalidResponse);

        assertTrue(result.isEmpty());
    }

    @Test
    public void given_emptyLockHolder_when_verifyWorkerHoldsLockForSharedSpace_then_logPhysicalMismatch() {
        final String failedLog = String.format("[PHYSICAL MISMATCH]: WorkerArn: %s doesn't have a lock for "
            + "sharedSpaceArn: %s because no worker does.", WORKER_ARN, SHARED_SPACE_DESTINATION_ARN);

        final Optional<PriorityQueueRecord> result =
            verifyWorkerHoldsLockForSharedSpace(WORKER_ARN, SHARED_SPACE_DESTINATION_ARN, Optional.empty());

        assertTrue(result.isEmpty());
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(failedLog)));
    }

    @Test
    public void given_noMatchingLockHolder_when_verifyWorkerHoldsLockForSharedSpace_then_logLockHolderMismatch() {
        final String expectedWorkerArn = "expectedWorkerArn";
        final String failedLog = String.format("[PHYSICAL MISMATCH] No actions can be taken because "
            + "workerArn: %s doesn't hold the lock for sharedSpaceArn: %s. The worker who actually holds the lock "
            + "is: %s", expectedWorkerArn, SHARED_SPACE_DESTINATION_ARN, WORKER_ARN);

        final Optional<PriorityQueueRecord> result = verifyWorkerHoldsLockForSharedSpace(
            expectedWorkerArn,
            SHARED_SPACE_DESTINATION_ARN,
            Optional.of(PriorityQueueRecord.builder().workerArn(WORKER_ARN).build()));

        assertTrue(result.isEmpty());
        assertTrue(mockedAppender.message.stream().anyMatch(msg -> msg.equals(failedLog)));
    }

    @Test
    public void given_matchingLockHolder_when_verifyWorkerHoldsLockForSharedSpace_then_returnLockHolderOptional() {
        final Optional<PriorityQueueRecord> optionalLockHolder = Optional.of(PriorityQueueRecord.builder()
            .workerArn(WORKER_ARN)
            .build());

        final Optional<PriorityQueueRecord> result =
            verifyWorkerHoldsLockForSharedSpace(WORKER_ARN, SHARED_SPACE_DESTINATION_ARN, optionalLockHolder);

        assertEquals(optionalLockHolder, result);
    }

    @Test
    public void given_robotOverTwoMeters_when_robotIsWithinTwoMeters_then_returnFalse() {
        final Polygon polygon =
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_1).get();
        final Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(0.0, 0.0));
        assertFalse(SharedSpaceUtils.robotIsWithinTwoMeters(point, polygon));
    }

    @Test
    public void given_robotWithinTwoMeters_when_robotIsWithinTwoMeters_then_returnTrue() {
        final Polygon polygon =
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_1).get();
        final Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(4.0, 0.0));
        assertTrue(SharedSpaceUtils.robotIsWithinTwoMeters(point, polygon));
    }

    @Test
    public void given_robotWithinSharedSpace_when_robotIsWithinTwoMeters_then_returnFalse() {
        final Polygon polygon =
            SharedSpaceUtils.createSharedSpacePolygon(SharedSpaceTestConstants.POLYGON_COORDS_1).get();
        final Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(5.0, 1.0));
        assertFalse(SharedSpaceUtils.robotIsWithinTwoMeters(point, polygon));
    }
}
