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

import com.amazon.iotroborunner.fmsg.types.RobotFleetType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

@Log4j2
abstract class ConfigurationValidator {
    private final int timeoutInMillis = 3000;
    private final Set<String> supportedFleetTypes = Arrays.stream(RobotFleetType.values())
            .map(fleetType -> fleetType.value.toLowerCase())
            .collect(Collectors.toUnmodifiableSet());

    protected boolean validateEndpoint(final String endpoint) {
        final boolean isValidInetAddress = new InetAddressValidator().isValid(endpoint);
        final boolean isValidUrl = new UrlValidator().isValid(endpoint);

        try {
            if (isValidUrl) {
                final URL urlObj = new URL(endpoint);
                final HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
                con.setRequestMethod("HEAD");

                // Set connection timeout
                con.setConnectTimeout(timeoutInMillis);
                con.connect();
            }
        } catch (IOException e) {
            log.error("Endpoint {} not available.", endpoint);
            return false;
        }
        return isValidInetAddress || isValidUrl;
    }

    protected boolean validateFleetType(final String fleetType) {
        return supportedFleetTypes.contains(fleetType.toLowerCase());
    }
}
