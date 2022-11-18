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

package com.amazon.iotroborunner.fmsg.types.sharedspace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * The object that is created when extracting vendorSharedSpace property
 * from additionalInformation of Destination resource stored in IoT RoboRunner.
 */
@Data
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorSharedSpace {
    @NonNull
    @JsonProperty("workerFleetArn")
    private String workerFleet;

    @JsonProperty("guid")
    private String guid;

    /**
     * Copy constructor to be used within other classes/methods for copying VendorSharedSpace object(s).
     */
    public VendorSharedSpace(final VendorSharedSpace vendorSharedSpace) {
        if (vendorSharedSpace == null) {
            throw new IllegalArgumentException("Copy-constructor's vendorSharedSpace argument cannot be null");
        }

        this.guid = vendorSharedSpace.getGuid();
        this.workerFleet = vendorSharedSpace.getWorkerFleet();
    }
}
