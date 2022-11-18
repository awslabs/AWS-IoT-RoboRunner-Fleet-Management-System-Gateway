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

import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createDestinationAdditionalInformationTestResource;
import static com.amazon.iotroborunner.fmsg.testhelpers.sharedspace.SharedSpaceTestUtils.createVendorSharedSpaceTestResource;

import com.amazon.iotroborunner.fmsg.testhelpers.TestConstants;
import com.amazon.iotroborunner.fmsg.testhelpers.TestUtils;
import com.amazon.iotroborunner.fmsg.types.sharedspace.DestinationAdditionalInformation;

import java.util.List;


/**
 * Constants used to simplify Destination creation and usage during testing.
 */
public final class DestinationTestConstants {

    /**
     * ARN for a Destination that doesn't represent a Shared Space.
     */
    public static final String STANDARD_DESTINATION_ARN =
            "arn:aws:iotroborunner:eu-central-1:accountId:destination/11111111-dddd-f3f3-3234-09s824ba0055";

    /**
     * ARN for a Destination represents a Shared Space.
     */
    public static final String SHARED_SPACE_DESTINATION_ARN =
            "arn:aws:iotroborunner:eu-central-1:accountId:destination/00000000-dddd-f3f3-3234-09s824ba0055";

    /**
     * Singleton holding a Shared Space Arn.
     */
    public static final List<String> SHARED_SPACE_ARN_SINGLETON = List.of(SHARED_SPACE_DESTINATION_ARN);

    /**
     * Additional Information field that doesn't match the Shared Space schema.
     */
    public static final String STANDARD_DESTINATION_ADDITIONAL_INFO = "AdditionalInfoStr";

    /**
     * Additional Information field that matches the Shared Space schema.
     */
    public static final DestinationAdditionalInformation SHARED_SPACE_DESTINATION_ADDITIONAL_INFO =
            createDestinationAdditionalInformationTestResource(List.of(
                    createVendorSharedSpaceTestResource(
                            TestUtils.generateSimilarArn(TestConstants.WORKER_FLEET_ARN),
                            TestUtils.generateId())));
    /**
     * Hidden Constructor.
     */

    private DestinationTestConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated");
    }
}
