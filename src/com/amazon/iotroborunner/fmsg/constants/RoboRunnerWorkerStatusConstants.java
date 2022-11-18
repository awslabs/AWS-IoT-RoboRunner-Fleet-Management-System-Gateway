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

/** Class defining constants related to RoboRunner Worker Status JSON fields. */
public final class RoboRunnerWorkerStatusConstants {
    /** The key for the schema version. */
    public static final String SCHEMA_VERSION_KEY = "schemaVersion";

    /** The schema version of the Worker Status JSON. */
    public static final String JSON_SCHEMA_VERSION = "1.0";

    /** The string for the battery level field in the additional transient properties. */
    public static final String BATTERY_LEVEL = "batteryLevel";

    /** The string for the degrees field of the orientation in the vendor additional transient properties. */
    public static final String DEGREES = "degrees";

    /** The string for the x position in the vendor additional transient properties. */
    public static final String VENDOR_X = "x";

    /** The string for the y position in the vendor additional transient properties. */
    public static final String VENDOR_Y = "y";

    /** The string for the vendor position field in the vendor additional transient properties. */
    public static final String VENDOR_POSITION = "vendorPosition";

    /** The string for the vendor state in the vendor additional transient properties. */
    public static final String VENDOR_STATE = "vendorState";

    /** The string for the vendor orientation in the vendor additional transient properties. */
    public static final String VENDOR_ORIENTATION = "vendorOrientation";

    /** The double used to make sure that the orientation is between 0 and 360. */
    public static final Double FULL_CIRCLE_DEGREES = 360.0;

    /** utility class only, does not allow instantitation of objects. */
    private RoboRunnerWorkerStatusConstants() {}
}
