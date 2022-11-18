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

package com.amazon.iotroborunner.fmsg.types.roborunner;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * Class representing RoboRunner Worker resource additional transient properties.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class WorkerAdditionalTransientProperties {
    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonProperty("customTransientProperties")
    private Map<String, String> customTransientProperties;

    /**
     * Gets the custom properties of a Worker resource.
     *
     * @return the copied map containing custom transient properties of a Worker
     */
    public Map<String, String> getCustomTransientProperties() {
        return Map.copyOf(customTransientProperties);
    }

    /**
     * Stores the custom properties of a Worker resource.
     *
     * @param customTransientProperties the map containing custom transient properties of a Worker
     */
    public void setCustomTransientProperties(final Map<String, String> customTransientProperties) {
        this.customTransientProperties = Map.copyOf(customTransientProperties);
    }

    /**
     * Converts the given string representing the worker
     * additional transient properties into the corresponding object.
     *
     * @param workerAdditionalTransientPropertiesStr the worker's additional transient properties as string
     * @return the worker's additional transient properties as object
     * @throws JsonProcessingException if there is an issue reading the JSON string
     */
    public static WorkerAdditionalTransientProperties readWorkerAdditionalTransientProperties(
            @NonNull final String workerAdditionalTransientPropertiesStr) throws JsonProcessingException {
        return new ObjectMapper().readValue(workerAdditionalTransientPropertiesStr,
                WorkerAdditionalTransientProperties.class);
    }
}
