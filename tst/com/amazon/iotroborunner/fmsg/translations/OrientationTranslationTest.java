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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.types.roborunner.OrientationOffset;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the OrientationTranslation class.
 */
public class OrientationTranslationTest {
    private final OrientationOffset orientationOffset = OrientationOffset.builder().degrees(5.0).build();
    private static final double DOUBLE_PRECISION_ERROR = 0.000001d;

    @Test
    public void given_validParameters_when_constructed_then_passes() {
        final OrientationTranslation translator = new OrientationTranslation(orientationOffset);

        assertNotNull(translator);
    }

    @Test
    public void given_nullOrientationOffset_when_constructued_then_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            final OrientationTranslation translator = new OrientationTranslation(null);
        });
    }

    @Test
    public void given_validOrientationOffset_when_getRoboRunnerOrientationFromFmsOrientation_passes() {
        final OrientationTranslation translator = new OrientationTranslation(orientationOffset);
        final double fmsOrientation = 50.50;

        final double roboRunnerOrientation = translator.getRoboRunnerOrientationFromFmsOrientation(fmsOrientation);

        assertEquals(fmsOrientation + orientationOffset.getDegrees(), roboRunnerOrientation);
    }

    @Test
    public void given_orientationGreaterThanMaxValue_when_getRoboRunnerOrientationFromFmsOrientation_returnsValid() {
        final OrientationTranslation translator = new OrientationTranslation(orientationOffset);
        final double fmsOrientation = 359.99;

        final double roboRunnerOrientation = translator.getRoboRunnerOrientationFromFmsOrientation(fmsOrientation);

        assertTrue(roboRunnerOrientation < 360.0);
    }

    @Test
    public void given_orientationLessThanMinValue_when_getRoboRunnerOrientationFromFmsOrientation_returnsValid() {
        final OrientationTranslation translator = new OrientationTranslation(orientationOffset);
        final double fmsOrientation = -90.0;

        final double roboRunnerOrientation = translator.getRoboRunnerOrientationFromFmsOrientation(fmsOrientation);

        assertTrue(roboRunnerOrientation >= 0.0);
    }

    @Test
    public void given_positiveOrientation_when_getPositiveOrientation_returnsTheSameOrientation() {
        final double orientation = 90.0;

        final double actualOrientationInDegrees = OrientationTranslation.getPositiveOrientation(orientation);

        assertEquals(orientation, actualOrientationInDegrees, DOUBLE_PRECISION_ERROR);
    }

    @Test
    public void given_negativeOrientation_when_getPositiveOrientation_returnsPositiveOrientation() {
        final double orientation = -90.0;
        final double expectedPositiveOrientation = orientation + 360.0;

        final double actualOrientationInDegrees = OrientationTranslation.getPositiveOrientation(orientation);

        assertEquals(expectedPositiveOrientation, actualOrientationInDegrees, DOUBLE_PRECISION_ERROR);
    }
}
