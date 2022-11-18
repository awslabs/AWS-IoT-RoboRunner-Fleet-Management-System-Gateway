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

package com.amazon.iotroborunner.fmsg.testhelpers.sharedspace;

import com.amazon.iotroborunner.fmsg.types.sharedspace.DestinationAdditionalInformation;
import com.amazon.iotroborunner.fmsg.types.sharedspace.SharedSpace;
import com.amazon.iotroborunner.fmsg.types.sharedspace.VendorSharedSpace;

import java.util.List;

import com.amazonaws.services.iotroborunner.model.Destination;
import com.amazonaws.services.iotroborunner.model.DestinationState;
import org.apache.commons.lang3.StringUtils;

/**
 * SharedSpaceTestUtils class contains general utility static methods to help to keep test
 * files clean and organized when it comes to creating shared space resources.
 */
public final class SharedSpaceTestUtils {

    /** 
     * A helper method to create a VendorSharedSpace test resource based on given worker fleet and guid if applicable.
     */
    public static VendorSharedSpace createVendorSharedSpaceTestResource(final String workerFleetArn,
                                                                        final String guid) {
        final VendorSharedSpace vendorSharedSpace = new VendorSharedSpace(workerFleetArn);
        if (!StringUtils.isBlank(guid)) {
            vendorSharedSpace.setGuid(guid);
        }

        return vendorSharedSpace;
    }

    /** 
     * A helper method to create a SharedSpace test resource based on given parameters.
     */
    public static SharedSpace createSharedSpaceTestResource(final String name, final String siteArn,
                                                            final String destinationArn, final String state,
                                                            final VendorSharedSpace vendorSharedSpace) {
        return SharedSpace.builder()
                .withName(name)
                .withSiteArn(siteArn)
                .withDestinationArn(destinationArn)
                .withState(state)
                .withVendorSharedSpace(vendorSharedSpace)
                .build();
    }

    /**
     * A helper method to create a Destination test resource based on given parameters.
     */
    public static Destination createDestinationTestResource(final String name, final String siteArn,
                                                            final String arn, final DestinationState state,
                                                            final String additionalInfoStr) {
        return new Destination()
                .withName(name)
                .withSite(siteArn)
                .withArn(arn)
                .withState(state)
                .withAdditionalFixedProperties(additionalInfoStr);
    }

    /**
     * A helper method to create a DestinationAdditionalInformation test resource based on given parameters.
     */
    public static DestinationAdditionalInformation createDestinationAdditionalInformationTestResource(
            final List<VendorSharedSpace> vendorSharedSpaces) {
        final DestinationAdditionalInformation additionalInfo = new DestinationAdditionalInformation();
        additionalInfo.setVendorSharedSpaces(vendorSharedSpaces);

        return additionalInfo;
    }

    /*
     * Utility class, don't allow instantiation.
     */
    private SharedSpaceTestUtils() {
        throw new UnsupportedOperationException("This class is for holding helper static methods and"
                + " should not be instantiated");
    }
}
