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

package com.amazon.iotroborunner.fmsg.types.mir;

import com.amazon.iotroborunner.fmsg.translations.MirRobotStatusDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/** Class representing the MiR robot status with information we want to store in RoboRunner. */
@NoArgsConstructor
@JsonDeserialize(using = MirRobotStatusDeserializer.class)
public class MirRobotStatus {
    /** Battery percentage of the robot. */
    @Getter
    @Setter
    private double batteryPercentage;

    /** Local x co-ordinate of the robot. */
    @Getter
    @Setter
    private double robotX;

    /** Local y co-ordinate of the robot. */
    @Getter
    @Setter
    private double robotY;

    /** Orientation of the robot. */
    @Getter
    @Setter
    private double orientation;

    /** Vendor state of the robot. */
    @Getter
    @NonNull
    @Setter
    private String state;
}
