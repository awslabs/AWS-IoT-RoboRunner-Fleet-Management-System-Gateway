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

import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;

import java.util.List;

import com.amazonaws.services.iotroborunner.AWSIoTRoboRunner;
import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.ListDestinationsRequest;
import com.amazonaws.services.iotroborunner.model.ListDestinationsResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility to retrieve IoT RoboRunner shared spaces.
 */
@RequiredArgsConstructor
@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class SharedSpaceClient {
    @NonNull
    private final AWSIoTRoboRunner rrClient;

    /**
     * Retrieves all shared spaces for given siteArn and fleetArn.
     */
    public List<SharedSpace> getAllSharedSpaces(final String siteArn, final String fleetArn)
            throws JsonProcessingException {

        if (StringUtils.isBlank(siteArn)) {
            throw new IllegalArgumentException("The siteArn cannot be null/empty when listing shared spaces");
        }

        if (StringUtils.isBlank(fleetArn)) {
            throw new IllegalArgumentException("The fleetArn cannot be null/empty when listing shared spaces");
        }

        ListDestinationsRequest listDestinationsRequest = new ListDestinationsRequest().withSite(siteArn);
        ListDestinationsResult listDestinationResult = rrClient.listDestinations(listDestinationsRequest);

        // Since not all destinations are shared spaces, some filtering will be required for each retrieved batch.
        List<Destination> destinations = listDestinationResult.getDestinations();
        final List<SharedSpace> allSharedSpaces = SharedSpaceUtils.extractSharedSpaces(destinations, fleetArn);

        String nextToken = listDestinationResult.getNextToken();
        while (nextToken != null) {
            listDestinationsRequest = new ListDestinationsRequest().withSite(siteArn).withNextToken(nextToken);
            listDestinationResult = rrClient.listDestinations(listDestinationsRequest);

            destinations = listDestinationResult.getDestinations();
            allSharedSpaces.addAll(SharedSpaceUtils.extractSharedSpaces(destinations, fleetArn));

            nextToken = listDestinationResult.getNextToken();
        }

        return allSharedSpaces;
    }
}
