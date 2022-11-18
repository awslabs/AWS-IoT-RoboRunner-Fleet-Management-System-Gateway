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
 * MiR API Endpoint Constants.
 */
public final class MirApiEndpointConstants {
    /**
     * Endpoint to Get Robot Status.
     */
    public static final String GET_ROBOT_STATUS_API_ENDPOINT = "/robots/";
    /**
     * Endpoint to Get Area Events.
     */
    public static final String GET_AREA_EVENTS_API_ENDPOINT = "/area_events/";
    /**
     * Endpoint suffix to block limit robot zones.
     */
    public static final String BLOCKED_API_ENDPOINT_SUFFIX = "/blocked";
    /**
     * Endpoint suffix to schedule a mission for a robot. Integration test use only.
     */
    public static final String MISSION_SCHEDULER_ENDPOINT = "/mission_scheduler";

    /**
     * Creates a fully constructed endpoint to get a status of given robot.
     *
     * @param robotId Robot's unique identifier to get the status for.
     * @return  Get Robot Status API Endpoint.
     */
    public static String getGetRobotStatusApiEndpoint(final int robotId) {
        return GET_ROBOT_STATUS_API_ENDPOINT + robotId;
    }

    /**
     * Creates a fully constructed endpoint to block a Shared Space.
     *
     * @param sharedSpaceId Vendor's unique identifier for the Shared Space.
     * @return Block Shared Space API endpoint.
     */
    public static String getBlockedSharedSpaceEndpoint(@NonNull final String sharedSpaceId) {
        return GET_AREA_EVENTS_API_ENDPOINT + sharedSpaceId + BLOCKED_API_ENDPOINT_SUFFIX;
    }

    /**
     * Creates a fully constructed endpoint to retrieve the details about the vendor Shared Space
     * (modelled via MiR's Area Events as a limit robot zone) for the provided Shared Space id.
     *
     * @param sharedSpaceId The Shared Space id to search for.
     * @return Endpoint to make a GET/area_events/{guid} call.
     */
    public static String getSharedSpaceEndpoint(@NonNull final String sharedSpaceId) {
        return GET_AREA_EVENTS_API_ENDPOINT + sharedSpaceId;
    }

    /**
     * Creates a fully constructed endpoint to gather the current status of the provided
     * robot.
     *
     * @param robotId Id of the robot to get status for.
     * @return Robot status endpoint.
     */
    public static String getRobotStatusEndpoint(@NonNull final String robotId) {
        return GET_ROBOT_STATUS_API_ENDPOINT + robotId;
    }

    /**
     * Hidden Constructor.
     */
    private MirApiEndpointConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated.");
    }
}
