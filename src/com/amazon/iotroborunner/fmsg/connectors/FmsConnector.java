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

package com.amazon.iotroborunner.fmsg.connectors;

import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandCallback;
import com.amazon.iotroborunner.fmsg.types.callback.FmsCommandType;

import java.time.Duration;

/** Interface for implementing classes that handle all communication between FMSG core and the FMS. */
public interface FmsConnector {
    /**
     * Method to inform connector that Worker Property updates are enabled and the required
     * resources must be created for it.
     */
    void setupWorkerPropertyUpdates();

    /**
     * Method for getting a robot's status from the FMS.
     *
     * @param robotId The robot's identifier within the FMS
     * @return        The worker status object created from the FMS response
     */
    WorkerStatus getRobotStatusById(String robotId);

    /**
     * Method to start collecting all robot statuses from the FMS.
     */
    void getAllRobotStatuses();

    /**
     * Method to stop collecting all robot statuses from the FMS.
     */
    void stopGetAllRobotStatuses();

    /**
     * Method to inform connector that Shared Space Management is enabled and the
     * required resources must be created for it.
     */
    void setupSharedSpaceManagement();

    /**
     * Method to allow a robot to pass through a shared space.
     *
     * @param workerArn      The ARN of the worker to allow through
     * @param sharedSpaceArn The ARN of the shared space to allow the robot through
     */
    void grantWorkerAccessToSharedSpace(String workerArn, String sharedSpaceArn);

    /**
     * Method to listen to Shared Spaces and report when a worker is waiting to
     * enter.
     *
     * @param vendorPollingDuration customer specified polling duration
     */
    void listenToSharedSpaces(Duration vendorPollingDuration);

    /**
     * Method that terminates the connector from listening to Shared Spaces.
     */
    void stopListeningToSharedSpaces();

    /**
     * Function to register a callback for a specific command type.
     *
     * @param command  The FMS command type to register the callback for
     * @param callback The callback being registered
     */
    void registerCallback(FmsCommandType command, FmsCommandCallback callback);

    /**
     * Function to unregister a callback for a specific command type.
     *
     * @param command  The command type to unregister a callback for
     * @param callback The callback to unregister
     */
    void unregisterCallback(FmsCommandType command, FmsCommandCallback callback);

}
