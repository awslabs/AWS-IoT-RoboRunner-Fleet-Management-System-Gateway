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

package com.amazon.iotroborunner.fmsg.config.validators;

import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;

/** Class to validate fields in the RoboRunner FMSG Configuration. */
public class FmsgCoreConfigurationValidator extends ConfigurationValidator {
    private static final Pattern SITE_ARN_REGEX_PATTERN =
                    Pattern.compile("^arn:aws:iotroborunner:[\\w-]+:\\w+:site/.*$");
    private static final String SITE_ARN = "siteArn";
    private static final String MAX_SHARED_SPACE_CROSSING_TIME_CONFIG_NAME = "maximumSharedSpaceCrossingTime";
    private static final String VENDOR_SHARED_SPACE_POLLING_INTERVAL_CONFIG_NAME = "vendorSharedSpacePollingInterval";

    private boolean validateSiteArn(final String arn) {
        final Matcher arnMatcher = SITE_ARN_REGEX_PATTERN.matcher(arn);
        return arnMatcher.matches();
    }

    private boolean validateMaximumSharedSpaceCrossingTime(final int seconds) {
        return (1 <= seconds && 900 >= seconds);
    }

    private boolean validateVendorSharedSpacePollingInterval(final int seconds) {
        return (1 <= seconds && 9 >= seconds);
    }

    /** Validate all necessary fields in the configuration object. Return ArrayList of invalid fields. */
    public ArrayList<String> validateConfiguration(@NonNull final FmsgCoreConfiguration config) {
        final ArrayList<String> invalidConfigFields = new ArrayList<String>();
        
        if (!validateSiteArn(config.getSiteArn())) {
            invalidConfigFields.add(SITE_ARN);
        }
        if (!validateMaximumSharedSpaceCrossingTime(config.getMaximumSharedSpaceCrossingTime())) {
            invalidConfigFields.add(MAX_SHARED_SPACE_CROSSING_TIME_CONFIG_NAME);
        }
        if (!validateVendorSharedSpacePollingInterval(config.getVendorSharedSpacePollingInterval())) {
            invalidConfigFields.add(VENDOR_SHARED_SPACE_POLLING_INTERVAL_CONFIG_NAME);
        }
        return invalidConfigFields;
    }
}
