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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazon.iotroborunner.fmsg.types.roborunner.PositionConversionCalibrationPoint;
import com.amazon.iotroborunner.fmsg.types.roborunner.ReferencePoint;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for the position translator translator module. */
public class PositionTranslationTest {

    private final List<PositionConversionCalibrationPoint> mapPoints = Arrays.asList(
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(0.0).ycoordinate(0.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build(),
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(0.0).ycoordinate(149.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build(),
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(279.0).ycoordinate(0.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build(),
            PositionConversionCalibrationPoint
                    .builder()
                    .vendorCoordinates(ReferencePoint.builder().xcoordinate(279.0).ycoordinate(149.0).build())
                    .roboRunnerCoordinates(ReferencePoint.builder().xcoordinate(33.77).ycoordinate(-84.35).build())
                    .build()
        );

    @Test
    public void given_validParameters_when_constructed_then_passes() {
        final PositionTranslation posTrans = new PositionTranslation(mapPoints);

        assertNotNull(posTrans);
    }

    @Test
    public void given_nullPositionConversionReferencePoints_when_constructued_then_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            final PositionTranslation posTrans = new PositionTranslation(null);
        });
    }

    @Test
    public void given_validPositionConversionReferencePoints_when_getRoboRunnerCoordinatesFromFmsCoordinates_passes() {
        final PositionTranslation posTrans = new PositionTranslation(mapPoints);

        final double[] roboRunnerCoordinates = posTrans.getRoboRunnerCoordinatesFromFmsCoordinates(76.45, 145.56);

        assertEquals(2, roboRunnerCoordinates.length);
        assertEquals(33.76999999999999, roboRunnerCoordinates[0]);
        assertEquals(-84.35000000000001, roboRunnerCoordinates[1]);
    }
}
