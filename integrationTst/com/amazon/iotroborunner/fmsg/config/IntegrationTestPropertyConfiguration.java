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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Class representing integration test specific configuration.
 */
@Builder
public class IntegrationTestPropertyConfiguration {
    @JsonProperty("fleetType")
    @Getter
    private String fleetType;

    @JsonProperty("testMissionIds")
    @Getter(AccessLevel.NONE)
    private Map<String, String> testMissionIds;

    @JsonProperty("coordinatesComparisonAllowedErrorInMeters")
    @Getter
    private double coordinatesComparisonAllowedErrorInMeters;

    @JsonProperty("workerUpdatePeriodInSeconds")
    @Getter
    private int workerUpdatePeriodInSeconds;

    @JsonProperty("workerUpdateAllowedDelayInSeconds")
    @Getter
    private int workerUpdateAllowedDelayInSeconds;

    @JsonProperty("orientationComparisonAllowedErrorInDegrees")
    @Getter
    private double orientationComparisonAllowedErrorInDegrees;

    /**
     * Method to get a copy of the testMissionIds fields.
     *
     * @return A copy of testMissionIds map.
     */
    public Map<String, String> getTestMissionIds() {
        return Map.copyOf(this.testMissionIds);
    }

    /**
     * Overriden builder to avoid internal representation warnings.
     */
    public static class IntegrationTestPropertyConfigurationBuilder {
        private Map<String, String> testMissionIds;

        /**
         * Custom setter to avoid exposing internal representation of the object.
         */
        public IntegrationTestPropertyConfigurationBuilder testMissionIds(
                final Map<String, String> testMissionIds) {
            this.testMissionIds = Map.copyOf(testMissionIds);
            return this;
        }
    }
}
