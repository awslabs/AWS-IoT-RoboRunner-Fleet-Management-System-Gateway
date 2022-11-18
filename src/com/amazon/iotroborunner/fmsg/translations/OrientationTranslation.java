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

import static com.amazon.iotroborunner.fmsg.constants.RoboRunnerWorkerStatusConstants.FULL_CIRCLE_DEGREES;

import com.amazon.iotroborunner.fmsg.types.roborunner.OrientationOffset;

import lombok.NonNull;

/**
 * Class that transforms FMS reported robot orientation to RoboRunner Worker orientation.
 */
public class OrientationTranslation {
    private double orientationOffset = 0.0;

    public OrientationTranslation(@NonNull final OrientationOffset orientationOffset) {
        this.orientationOffset = orientationOffset.getDegrees();
    }

    /**
     * Transform FMS orientation to RoboRunner orientation using the orientation offset.
     *
     * @param fmsOrientation    orientation reported by the FMS.
     * @return                  transformed orientation value.
     */
    public double getRoboRunnerOrientationFromFmsOrientation(final double fmsOrientation) {
        double roboRunnerOrientation = fmsOrientation + this.orientationOffset;

        if (roboRunnerOrientation < 0.0) {
            roboRunnerOrientation += FULL_CIRCLE_DEGREES;
        }
        if (roboRunnerOrientation >= FULL_CIRCLE_DEGREES) {
            roboRunnerOrientation -= FULL_CIRCLE_DEGREES;
        }

        return roboRunnerOrientation;
    }

    /**
     * Converts the negative orientation value in degrees to positive.
     *
     * @param orientation   original orientation that will be converted to positive number if negative.
     * @return              converted to positive number orientation value in degrees.
     */
    public static double getPositiveOrientation(final double orientation) {
        return orientation < 0 ? orientation + FULL_CIRCLE_DEGREES : orientation;
    }
}
