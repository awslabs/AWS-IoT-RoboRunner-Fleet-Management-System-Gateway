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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

/**
 * This object is simulating the Destination resource representing a shared space from IoT RoboRunner
 * but with the additionalInformation property being of type DestinationAdditionalInformation instead of
 * a string representing a json. This will allow using the destinations/shared spaces information
 * without needing to read/map json string to POJO every time.
 */
@Data
@Builder(setterPrefix = "with")
@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
        justification = "@Data generates redundant null checks in equals(), hashcode() methods that cannot be avoided")
public class SharedSpace {
    @NonNull
    private final String destinationArn;
    @NonNull
    private final String siteArn;
    private final String state;
    @NonNull
    private final String name;

    @Getter(AccessLevel.NONE)
    private final VendorSharedSpace vendorSharedSpace;

    /**
     * Custom getter to avoid exposing internal representation of the object.
     */
    public VendorSharedSpace getVendorSharedSpace() {
        return new VendorSharedSpace(this.vendorSharedSpace);
    }

    /**
     *  Overridden shared space builder for lombok to provide a custom setter in order to
     *  avoid exposing internal representation of the object.
     */
    public static class SharedSpaceBuilder {
        private VendorSharedSpace vendorSharedSpace;

        /**
         * Custom setter to avoid exposing internal representation of the object.
         */
        public SharedSpaceBuilder withVendorSharedSpace(final VendorSharedSpace vendorSharedSpace) {
            this.vendorSharedSpace = new VendorSharedSpace(vendorSharedSpace);
            return this;
        }
    }
}
