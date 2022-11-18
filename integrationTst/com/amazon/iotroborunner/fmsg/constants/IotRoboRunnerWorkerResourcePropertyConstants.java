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

package com.amazon.iotroborunner.fmsg.constants;

import lombok.NonNull;

/**
 * IoT RoboRunner resource content constants.
 */
public final class IotRoboRunnerWorkerResourcePropertyConstants {
    /**
     * Represents the vendor robot's state in IoT RoboRunner Worker resource
     * as part of the vendor properties.
     */
    public static final String VENDOR_PROPERTIES_STATE_KEY = "state";
    /**
     * Represents the vendor robot's position in IoT RoboRunner Worker resource
     * as part of the vendor properties.
     */
    public static final String VENDOR_PROPERTIES_POSITION_KEY = "position";
    /**
     * Represents the vendor robot's position's X coordinate.
     */
    public static final String VENDOR_PROPERTIES_POSITION_X_KEY = "x";
    /**
     * Represents the vendor robot's position's Y coordinate.
     */
    public static final String VENDOR_PROPERTIES_POSITION_Y_KEY = "y";
    /**
     * Represents the vendor robot's orientation in IoT RoboRunner Worker resource
     * as part of the vendor properties.
     */
    public static final String ORIENTATION_DEGREES_KEY = "degrees";

    /**
     * Vendor robot's possible state values.
     */
    public enum VendorPropertiesState {
        EXECUTING("Executing"),
        READY("Ready");

        public final String value;

        VendorPropertiesState(@NonNull final String val) {
            this.value = val;
        }
    }

    /**
     * Hidden Constructor.
     */
    private IotRoboRunnerWorkerResourcePropertyConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated.");
    }
}
