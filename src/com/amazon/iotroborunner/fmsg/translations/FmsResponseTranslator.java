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

package com.amazon.iotroborunner.fmsg.translations;

import com.amazon.iotroborunner.fmsg.types.WorkerStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;

/**
 * Interface for implementing classes that deserialize FMS responses into type objects.
 */
public interface FmsResponseTranslator {
    /**
     * Get the FMS response translated to a RoboRunner Worker Status.
     *
     * @param robotId               The ID of the FMS robot that is being updated in RoboRunner.
     * @param fmsResponse           The string body of the FMS response
     * @param positionTranslator    The PositionTranslation object used to transform FMS reported robot position
     * @param orientationTranslator The OrientationTranslation object used to transform FMS reported robot orientation
     * @return                      RoboRunner Worker Status object built from FMS response
     * @throws JsonProcessingException If there is an issue reading the JSON String
     */
    WorkerStatus getWorkerStatusFromFmsResponse(
            @NonNull String robotId,
            @NonNull String fmsResponse,
            PositionTranslation positionTranslator,
            OrientationTranslation orientationTranslator) throws JsonProcessingException;
}
