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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.ListDestinationsRequest;
import com.amazonaws.services.iotroborunner.model.ListDestinationsResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the Shared Space client module. */
@ExtendWith(MockitoExtension.class)
public class SharedSpaceClientTest {
    @Mock
    private AWSIoTRoboRunner mockRrClient;

    @Mock
    private Destination mockDestination1;

    @Mock
    private Destination mockDestination2;

    @Mock
    private Destination mockDestination3;

    @Mock
    private SharedSpace sharedSpace1;

    @Mock
    private SharedSpace sharedSpace2;

    @Mock
    private SharedSpace sharedSpace3;

    private SharedSpaceClient sharedSpaceClient;

    @BeforeEach
    public void setUp() {
        sharedSpaceClient = new SharedSpaceClient(mockRrClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void given_NullOrEmptySiteArn_when_getAllSharedSpaces_then_throwsIllegalArgumentException(final String siteArn) {
        assertThrows(IllegalArgumentException.class, () -> {
            sharedSpaceClient.getAllSharedSpaces(siteArn, TestConstants.WORKER_FLEET_ARN);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    void given_NullOrEmptyFleetArn_when_getAllSharedSpaces_then_throwsIllegalArgumentException(final String fleetArn) {
        assertThrows(IllegalArgumentException.class, () -> {
            sharedSpaceClient.getAllSharedSpaces(TestConstants.SITE_ARN, fleetArn);
        });
    }

    @Test
    void given_siteArnAndFleetArn_when_getAllSharedSpaces_then_returnsListOfSharedSpaces()
            throws JsonProcessingException {
        // Given
        final ListDestinationsResult responseWithNextToken = new ListDestinationsResult()
                .withNextToken("nextToken")
                .withDestinations(List.of(mockDestination1, mockDestination2));

        final ListDestinationsResult responseWithoutNextToken = new ListDestinationsResult()
                .withNextToken(null)
                .withDestinations(List.of(mockDestination3));

        when(mockRrClient.listDestinations(any(ListDestinationsRequest.class)))
                .thenReturn(responseWithNextToken)
                .thenReturn(responseWithoutNextToken);

        try (MockedStatic<SharedSpaceUtils> mockedStatic = mockStatic(SharedSpaceUtils.class)) {
            mockedStatic.when(() -> SharedSpaceUtils.extractSharedSpaces(anyList(), anyString()))
                    .thenReturn(Stream.of(sharedSpace1, sharedSpace2).collect(Collectors.toList()))
                    .thenReturn(Stream.of(sharedSpace3).collect(Collectors.toList()));

            // Testing
            final List<SharedSpace> sharedSpaces = sharedSpaceClient.getAllSharedSpaces(
                    TestConstants.SITE_ARN, TestConstants.WORKER_FLEET_ARN);

            // Verification
            assertEquals(3, sharedSpaces.size());
            assertTrue(sharedSpaces.containsAll(List.of(sharedSpace1, sharedSpace2, sharedSpace3)));
        }
    }

    @Test
    void given_siteArnAndFleetArn_when_getAllSharedSpaces_then_returnsEmptyList() throws JsonProcessingException {
        // Given
        final ListDestinationsResult responseWithoutNextToken = new ListDestinationsResult()
                .withDestinations(List.of(mockDestination1, mockDestination2, mockDestination3));

        when(mockRrClient.listDestinations(any(ListDestinationsRequest.class)))
                .thenReturn(responseWithoutNextToken);

        try (MockedStatic<SharedSpaceUtils> mockedStatic = mockStatic(SharedSpaceUtils.class)) {
            mockedStatic.when(() -> SharedSpaceUtils.extractSharedSpaces(anyList(), anyString()))
                    .thenReturn(Collections.emptyList());

            // Testing
            final List<SharedSpace> result = sharedSpaceClient.getAllSharedSpaces(
                    TestConstants.SITE_ARN, TestConstants.WORKER_FLEET_ARN);

            // Verification
            assertEquals(0, result.size());
        }
    }
}
