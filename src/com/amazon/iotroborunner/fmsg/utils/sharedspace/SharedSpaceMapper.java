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
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Set of utilities that map vendor shared space resources to their corresponding IoT RoboRunner resources.
 */
@RequiredArgsConstructor
public final class SharedSpaceMapper {
    @NonNull
    private final SharedSpaceClient sharedSpaceClient;

    /**
     * Utility method creates a mapping between the vendor shared space to IoT RoboRunner destination resource.
     */
    public Map<String, String> createVendorSharedSpaceToDestinationMap(final String siteArn,
                                                                       final String fleetArn)
            throws JsonProcessingException {
        if (StringUtils.isBlank(siteArn)) {
            throw new IllegalArgumentException("The siteArn cannot be null/empty"
                    + " when creating a vendor shared space id to destination arn map");
        }

        if (StringUtils.isBlank(fleetArn)) {
            throw new IllegalArgumentException("The fleetArn cannot be null/empty"
                    + " when creating a vendor shared space id to destination arn map");
        }

        final Map<String, String> vendorSharedSpaceIdToDestinationArnMap = new ConcurrentHashMap<>();

        final List<SharedSpace> sharedSpaces = sharedSpaceClient.getAllSharedSpaces(siteArn, fleetArn);

        for (final SharedSpace sharedSpace : sharedSpaces) {
            final VendorSharedSpace vendorSharedSpace = sharedSpace.getVendorSharedSpace();

            // The map needs to contain shared space mappings per fleet.
            if (vendorSharedSpace.getWorkerFleet().equals(fleetArn)) {
                final String vendorSharedSpaceId = getVendorSharedSpaceId(sharedSpace);
                vendorSharedSpaceIdToDestinationArnMap.put(vendorSharedSpaceId, sharedSpace.getDestinationArn());
            }
        }

        return vendorSharedSpaceIdToDestinationArnMap;
    }

    /*
     * A helper method to return vendorSharedSpaceId from a provided SharedSpace
     * object. The vendorSharedSpaceId is dependent on the vendor's API
     * requirements. If a GUID was provided in the resource it will be returned as
     * the id, otherwise we default to the name of the vendor shared space.
     */
    private String getVendorSharedSpaceId(@NonNull final SharedSpace sharedSpace) {
        final String guid = sharedSpace.getVendorSharedSpace().getGuid();
        if (!StringUtils.isBlank(guid)) {
            return guid;
        }

        return sharedSpace.getName();
    }
}
