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

import static com.amazon.iotroborunner.fmsg.types.RobotFleetType.SIMULATED;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;

/**
 * FmsgConnectorConfigurationValidator class is designed to validate
 * property values from FmsgConnectorConfiguration.json file.
 */
public class FmsgConnectorConfigurationValidator extends ConfigurationValidator {
    private static final Pattern WORKER_FLEET_ARN_REGEX_PATTERN =
        Pattern.compile("^arn:aws:iotroborunner:[\\w-]+:\\w+:site/[\\w-]+/worker-fleet/.*$");

    /**
     *  Validates given worker fleet arn.
     *
     * @param workerFleetArn the unique identifier of a worker fleet that needs to be validated
     * @return true if given worker fleet arn is valid, false otherwise
     */
    private boolean validateWorkerFleetArn(final String workerFleetArn) {
        final Matcher arnMatcher = WORKER_FLEET_ARN_REGEX_PATTERN.matcher(workerFleetArn);
        return arnMatcher.matches();
    }

    /**
     * Validates the property values from FmsgConnectorConfiguration.json file.
     *
     * @param config the config property values from which need to be validated
     * @return true if the configuration file is valid, false otherwise
     */
    public boolean validateConfiguration(@NonNull final FmsgConnectorConfiguration config) {
        final boolean isFleetTypeValid = validateFleetType(config.getFleetType());
        final boolean isWorkerFleetArnValid = validateWorkerFleetArn(config.getWorkerFleetArn());
        final boolean iEndpointValid = config.getFleetType().equals(SIMULATED.value)
                || validateEndpoint(config.getApiEndpoint());

        return isFleetTypeValid && isWorkerFleetArnValid && iEndpointValid;
    }
}