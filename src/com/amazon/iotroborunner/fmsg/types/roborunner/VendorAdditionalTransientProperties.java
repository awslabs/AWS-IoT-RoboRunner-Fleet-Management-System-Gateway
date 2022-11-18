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

import com.amazonaws.services.iotroborunner.model.CartesianCoordinates;
import com.amazonaws.services.iotroborunner.model.Orientation;
import com.amazonaws.services.iotroborunner.model.PositionCoordinates;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Class representing RoboRunner Worker resource additional transient properties.
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class VendorAdditionalTransientProperties {
    @Getter
    @Setter
    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @Getter
    @Setter
    @JsonProperty("vendorState")
    private String vendorState;

    @Getter
    @Setter(AccessLevel.NONE)
    @JsonProperty("vendorPosition")
    private PositionCoordinates vendorPosition;

    @Getter
    @Setter
    @JsonProperty("vendorOrientation")
    private Orientation vendorOrientation;

    /**
     * A custom setter to be used when initializing VendorAdditionalTransientProperties object.
     *
     * @param cartesianCoordinates the cartesian coordinates of the position
     */
    @JsonProperty("vendorPosition")
    public void setVendorPosition(@NonNull final CartesianCoordinates cartesianCoordinates) {
        this.vendorPosition = new PositionCoordinates().withCartesianCoordinates(cartesianCoordinates);
    }

    /**
     * Converts the given string representing the vendor
     * additional transient properties into the corresponding object.
     *
     * @param vendorAdditionalTransientPropertiesStr the vendor additional transient properties represented as string
     * @return VendorAdditionalTransientProperties the vendor additional transient properties as an object
     * @throws JsonProcessingException if there is an issue reading the JSON String
     */
    public static VendorAdditionalTransientProperties readVendorAdditionalTransientProperties(
            @NonNull final String vendorAdditionalTransientPropertiesStr) throws JsonProcessingException {
        return new ObjectMapper().readValue(vendorAdditionalTransientPropertiesStr,
                VendorAdditionalTransientProperties.class);
    }
}
