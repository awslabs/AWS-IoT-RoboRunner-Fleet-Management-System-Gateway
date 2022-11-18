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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Class representing RoboRunner WorkerFleet resource additional fixed properties.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class WorkerFleetAdditionalFixedProperties {
    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @JsonProperty("positionConversion")
    private List<PositionConversionCalibrationPoint> positionConversion;

    @JsonProperty("orientationOffset")
    private OrientationOffset orientationOffset;

    /**
     * Custom getter to avoid exposing internal representation of the object.
     */
    public List<PositionConversionCalibrationPoint> getPositionConversion() {
        if (this.positionConversion != null) {
            return List.copyOf(this.positionConversion);
        }
        return null;
    }

    /**
     * Custom setter to avoid exposing internal representation of the object.
     */
    public void setPositionConversion(@NonNull final List<PositionConversionCalibrationPoint> mapPoints) {
        this.positionConversion = List.copyOf(mapPoints);
    }
}
