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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Class representing FMSG connector configuration.
 */
@Builder
public class FmsgConnectorConfiguration {
    /** Robot fleet type, for example MIR. */
    @JsonProperty("fleetType")
    @NonNull
    @Getter
    private String fleetType;

    /** WorkerFleet ARN within RoboRunner for fleet type. */
    @JsonProperty("workerFleetArn")
    @NonNull
    @Getter
    private String workerFleetArn;

    /** The endpoint of the FMS API. */
    @JsonProperty("apiEndpoint")
    @NonNull
    @Getter
    private String apiEndpoint;

    /** The secret name within SecretsManager for this fleet type. */
    @JsonProperty("apiSecretName")
    @NonNull
    @Getter
    private String apiSecretName;

    /** Optional additional configuration needed for the connector to correctly communicate with the FMS. */
    @JsonProperty("additionalConfiguration")
    private Map<String, String> additionalConfiguration;

    /** The feature flag to enable the FMS connector. */
    @JsonProperty("enableConnector")
    @Builder.Default
    @Getter
    private boolean enableConnector = false;

    /**
     * Extract the site ARN from the worker fleet ARN.
     *
     * @return The site ARN associated with the worker fleet ARN
     */
    public String getSiteArn() {
        final Pattern siteArnPattern = Pattern.compile("arn:aws:iotroborunner:[\\w-]+:\\w+:site/[\\w-]+");
        final Matcher siteArnMatcher = siteArnPattern.matcher(this.workerFleetArn);
        if (siteArnMatcher.find()) {
            return siteArnMatcher.group(0);
        }
        return null;
    }

    /**
     * Extract the AWS region from the worker fleet ARN.
     *
     * @return The AWS region associated with the worker fleet ARN
     */
    public String getAwsRegion() {
        final Pattern regionPattern = Pattern.compile("(?<=roborunner:).*?(?=:\\w+:site)");
        final Matcher regionMatcher = regionPattern.matcher(this.workerFleetArn);
        if (regionMatcher.find()) {
            return regionMatcher.group(0);
        }
        return null;
    }

    /**
     * Method to get a copy of the additionalConfiguration fields.
     *
     * @return A copy of the additionalConfiguration map
     */
    public Map<String, String> getAdditionalConfiguration() {
        return Map.copyOf(this.additionalConfiguration);
    }

    /** Overriden builder to avoid internal representation warnings. */
    public static class FmsgConnectorConfigurationBuilder {
        private Map<String, String> additionalConfiguration;

        /** Custom setter to avoid exposing internal representation of the object.  */
        public FmsgConnectorConfigurationBuilder additionalConfiguration(
                final Map<String, String> additionalConfiguration) {
            this.additionalConfiguration = Map.copyOf(additionalConfiguration);
            return this;
        }
    }
}
