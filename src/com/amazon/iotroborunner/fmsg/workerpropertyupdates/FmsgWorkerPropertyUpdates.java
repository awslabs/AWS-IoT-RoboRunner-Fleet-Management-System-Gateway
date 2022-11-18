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

package com.amazon.iotroborunner.fmsg.workerpropertyupdates;

import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;

import java.util.Map;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Singleton class that contains the logic for FMSG Worker Property Updates.
 */
@Log4j2
public final class FmsgWorkerPropertyUpdates {
    /**
     * Hidden Constructor.
     */
    private FmsgWorkerPropertyUpdates() {
    }

    /**
     * Calls getAllRobotStatuses() for each connector created.
     * getAllRobotStatuses is the logic of getting the status, extracting location information,
     * converting to RoboRunner's format, and sending the update to RoboRunner.
     *
     * @param connectors The list of connectors being used
     */
    public static void startWorkerPropertyUpdates(@NonNull final Map<String, FmsConnector> connectors) {
        log.info("Starting Worker Property Updates application");

        for (final FmsConnector connector : connectors.values()) {
            connector.getAllRobotStatuses();
        }
    }
}
