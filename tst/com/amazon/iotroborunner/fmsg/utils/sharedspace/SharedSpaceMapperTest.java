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

import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createSharedSpaceTestResource;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createVendorSharedSpaceTestResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.testhelpers.TestUtils;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.iotroborunner.model.DestinationState;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the Shared Space Mapper module. */
@ExtendWith(MockitoExtension.class)
public class SharedSpaceMapperTest {
    @Mock
    private SharedSpaceClient mockSharedSpaceClient;

    private SharedSpaceMapper sharedSpaceMapper;

    @BeforeEach
    public void setUp() {
        sharedSpaceMapper = new SharedSpaceMapper(mockSharedSpaceClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void given_NullOrEmptySiteArn_when_createVendorSharedSpaceToDestinationMap_then_throwsIllegalArgumentException(
                final String testSiteArnValue) {
        assertThrows(IllegalArgumentException.class, () -> {
            sharedSpaceMapper.createVendorSharedSpaceToDestinationMap(
                        testSiteArnValue, TestConstants.WORKER_FLEET_ARN);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    void given_NullOrEmptyFleetArn_when_createVendorSharedSpaceToDestinationMap_then_throwsIllegalArgumentException(
            final String testWorkerFleetArnValue) {
        assertThrows(IllegalArgumentException.class, () -> {
            sharedSpaceMapper.createVendorSharedSpaceToDestinationMap(TestConstants.SITE_ARN, testWorkerFleetArnValue);
        });
    }

    @Test
    void given_multipleSharedSpacesWithNameOrGuidAndFleetArn_when_createVendorSharedSpaceToDestinationMap_then_returnsMapWithGuidKeysOnly()
            throws JsonProcessingException {
        // Given
        final String vendor1SharedSpace1Guid = TestUtils.generateId();
        final VendorSharedSpace vendor1SharedSpace1 = createVendorSharedSpaceTestResource(
                TestConstants.WORKER_FLEET_ARN, vendor1SharedSpace1Guid);

        final String vendor1SharedSpace2Guid = TestUtils.generateId();
        final VendorSharedSpace vendor1SharedSpace2 = createVendorSharedSpaceTestResource(
                TestConstants.WORKER_FLEET_ARN, vendor1SharedSpace2Guid);

        final VendorSharedSpace vendor2SharedSpace = createVendorSharedSpaceTestResource(
                TestUtils.generateSimilarArn(TestConstants.WORKER_FLEET_ARN), null);

        final SharedSpace sharedSpace1 = createSharedSpaceTestResource("Vendor1SharedSpace1", TestConstants.SITE_ARN,
                TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendor1SharedSpace1);

        final String sharedSpace2Arn = TestUtils.generateSimilarArn(TestConstants.DESTINATION_ARN);
        final SharedSpace sharedSpace2 = createSharedSpaceTestResource("Vendor1SharedSpace2", TestConstants.SITE_ARN,
                sharedSpace2Arn, DestinationState.ENABLED.toString(), vendor1SharedSpace2);

        final SharedSpace sharedSpace3 = createSharedSpaceTestResource("Vendor2SharedSpace", TestConstants.SITE_ARN,
                TestUtils.generateSimilarArn(TestConstants.DESTINATION_ARN), DestinationState.ENABLED.toString(),
                vendor2SharedSpace);

        final List<SharedSpace> sharedSpaces = new ArrayList<>();
        sharedSpaces.addAll(Stream.of(sharedSpace1, sharedSpace2, sharedSpace3).collect(Collectors.toList()));

        when(mockSharedSpaceClient.getAllSharedSpaces(TestConstants.SITE_ARN, TestConstants.WORKER_FLEET_ARN))
                .thenReturn(sharedSpaces);

        // Testing
        final Map<String, String> vendorSharedSpaceIdToDestinationArnMap = sharedSpaceMapper
                .createVendorSharedSpaceToDestinationMap(TestConstants.SITE_ARN, TestConstants.WORKER_FLEET_ARN);

        // Verification
        assertEquals(2, vendorSharedSpaceIdToDestinationArnMap.size());
        assertTrue(List.of(vendor1SharedSpace1Guid, vendor1SharedSpace2Guid)
                .containsAll(vendorSharedSpaceIdToDestinationArnMap.keySet().stream().collect(Collectors.toList())));
        assertEquals(TestConstants.DESTINATION_ARN,
                vendorSharedSpaceIdToDestinationArnMap.get(vendor1SharedSpace1Guid));
        assertEquals(sharedSpace2Arn, vendorSharedSpaceIdToDestinationArnMap.get(vendor1SharedSpace2Guid));
    }

    @Test
    void given_multipleSharedSpacesWithNameOrGuidAndFleetArn_when_createVendorSharedSpaceToDestinationMap_then_returnsMapWithNameKeysOnly()
            throws JsonProcessingException {
        // Given
        final VendorSharedSpace vendor1SharedSpace = createVendorSharedSpaceTestResource(
                TestUtils.generateSimilarArn(TestConstants.WORKER_FLEET_ARN), TestUtils.generateId());

        final VendorSharedSpace vendor2SharedSpace = createVendorSharedSpaceTestResource(
                TestConstants.WORKER_FLEET_ARN, null);

        final SharedSpace sharedSpace1 = createSharedSpaceTestResource("Vendor1SharedSpace", TestConstants.SITE_ARN,
                TestUtils.generateSimilarArn(TestConstants.DESTINATION_ARN), DestinationState.ENABLED.toString(),
                vendor1SharedSpace);

        final String vendor2SharedSpaceName = "Vendor2SharedSpaceName";
        final SharedSpace sharedSpace2 = createSharedSpaceTestResource(vendor2SharedSpaceName, TestConstants.SITE_ARN,
                TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendor2SharedSpace);

        final List<SharedSpace> sharedSpaces = new ArrayList<>();
        sharedSpaces.addAll(Stream.of(sharedSpace1, sharedSpace2).collect(Collectors.toList()));

        when(mockSharedSpaceClient.getAllSharedSpaces(TestConstants.SITE_ARN, TestConstants.WORKER_FLEET_ARN))
                .thenReturn(sharedSpaces);

        // Testing
        final Map<String, String> vendorSharedSpaceIdToDestinationArnMap = sharedSpaceMapper
                .createVendorSharedSpaceToDestinationMap(TestConstants.SITE_ARN,
                        TestConstants.WORKER_FLEET_ARN);

        // Verification
        assertEquals(1, vendorSharedSpaceIdToDestinationArnMap.size());
        assertEquals(TestConstants.DESTINATION_ARN, vendorSharedSpaceIdToDestinationArnMap.get(vendor2SharedSpaceName));
    }

    @Test
    void given_duplicateSharedSpaces_when_createVendorSharedSpaceToDestinationMap_then_returnsMapWithOneEntry()
            throws JsonProcessingException {
        // Given
        final VendorSharedSpace vendor1SharedSpace = createVendorSharedSpaceTestResource(
                TestConstants.WORKER_FLEET_ARN, null);

        final VendorSharedSpace vendor2SharedSpace = createVendorSharedSpaceTestResource(
                TestConstants.WORKER_FLEET_ARN, null);

        final String vendorShareSpaceName = "VendorSharedSpace";
        final SharedSpace sharedSpace1 = createSharedSpaceTestResource(vendorShareSpaceName, TestConstants.SITE_ARN,
                TestUtils.generateSimilarArn(TestConstants.DESTINATION_ARN), DestinationState.ENABLED.toString(),
                vendor1SharedSpace);

        final SharedSpace sharedSpace2 = createSharedSpaceTestResource(vendorShareSpaceName, TestConstants.SITE_ARN,
                TestConstants.DESTINATION_ARN, DestinationState.ENABLED.toString(), vendor2SharedSpace);

        final List<SharedSpace> sharedSpaces = new ArrayList<>();
        sharedSpaces.addAll(Stream.of(sharedSpace1, sharedSpace2).collect(Collectors.toList()));

        when(mockSharedSpaceClient.getAllSharedSpaces(TestConstants.SITE_ARN, TestConstants.WORKER_FLEET_ARN))
                .thenReturn(sharedSpaces);

        // Testing
        final Map<String, String> vendorSharedSpaceIdToDestinationArnMap =
                sharedSpaceMapper.createVendorSharedSpaceToDestinationMap(TestConstants.SITE_ARN,
                        TestConstants.WORKER_FLEET_ARN);

        // Verification
        assertEquals(1, vendorSharedSpaceIdToDestinationArnMap.size());
        assertEquals(TestConstants.DESTINATION_ARN, vendorSharedSpaceIdToDestinationArnMap.get(vendorShareSpaceName));
    }
}
