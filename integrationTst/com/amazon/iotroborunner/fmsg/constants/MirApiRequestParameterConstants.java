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

/**
 * MiR API request parameter constants.
 */
public final class MirApiRequestParameterConstants {
    /**
     * A parameter to provide unique identifier of a mission.
     */
    public static final String MISSION_ID_PARAM = "mission_id";
    /**
     * A parameter to provide a description of a mission.
     */
    public static final String MISSION_DESCRIPTION_PARAM = "description";
    /**
     * A parameter to provide the priority of the mission that is being scheduled.
     */
    public static final String MISSION_EXECUTION_PRIORITY_PARAM = "priority";
    /**
     * A parameter to indicate if scheduled mission is high priority.
     */
    public static final String MISSION_EXECUTION_PRIORITY_HIGH_PARAM = "high_priority";
    /**
     * A parameter to provide the earliest time a mission should be executed.
     */
    public static final String MISSION_EARLIEST_START_TIME_PARAM = "earliest_start_time";
    /**
     * A parameter to provide unique identifier of a MiR robot.
     */
    public static final String VENDOR_WORKER_ID_PARAM = "robot_id";

    /**
     * Hidden Constructor.
     */
    private MirApiRequestParameterConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated.");
    }
}
