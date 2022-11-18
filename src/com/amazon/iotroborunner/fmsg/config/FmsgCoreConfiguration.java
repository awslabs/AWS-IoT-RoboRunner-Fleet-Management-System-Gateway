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

package com.amazon.iotroborunner.fmsg.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Class representing RoboRunner related configuration.
 */
@AllArgsConstructor
@Builder
public class FmsgCoreConfiguration {
    /**
     * Site resource ARN that represents the facility.
     */
    @Getter
    @NonNull
    private String siteArn;

    /**
     * Feature flag to enable continuous RoboRunner Worker Status updates.
     * This configuration is optional and is set to false by default.
     */
    @Builder.Default
    @Getter
    private boolean workerPropertyUpdatesEnabled = false;

    /**
     * Feature flag to enable shared space management.
     * This configuration is optional and is set to false by default.
     */
    @Builder.Default
    @Getter
    private boolean spaceManagementEnabled = false;

    /**
     * Feature flag to set the maximum shared space crossing time.
     * This configuration is optional and is set to 300 seconds by default.
     */
    @Builder.Default
    @Getter
    private int maximumSharedSpaceCrossingTime = 300;

    /**
     * Feature flag to set the vendor shared space polling interval.
     * This configuration is optional and is set to 3 seconds by default.
     */
    @Builder.Default
    @Getter
    private int vendorSharedSpacePollingInterval = 3;

    /**
     * Extract the AWS region from the site ARN.
     *
     * @return The AWS region associated with the site ARN
     */
    public String getAwsRegion() {
        final Pattern regionPattern = Pattern.compile("(?<=roborunner:).*?(?=:\\w+:site)");
        final Matcher regionMatcher = regionPattern.matcher(this.siteArn);
        if (regionMatcher.find()) {
            return regionMatcher.group(0);
        }
        return null;
    }
}
