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

package com.amazon.iotroborunner.fmsg.testutils.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.NonNull;

/**
 * Collection of helper methods to assist with integration test logic.
 */
public final class TestHelpers {
    /**
     * Calculates and returns the straight line distance between given
     * points represented by (x,y) in Cartesian Coordinate System.
     */
    public static double getDistanceInCartesianCoordinateSystem(final double x1, final double y1,
                                                                final double x2, final double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    /**
     * Removes duplications in the list of strings where the duplicates are in sequence.
     */
    public static List<String> removeSequentialDuplicates(@NonNull final List<String> strList) {
        final List<String> deduplicateList = new ArrayList<>();

        if (strList.isEmpty()) {
            return deduplicateList;
        }

        if (strList.size() == 1) {
            return List.copyOf(strList);
        }

        deduplicateList.add(strList.get(0));

        for (int i = 1; i < strList.size(); i++) {
            final String currStr = strList.get(i);
            if (!strList.get(i).equals(deduplicateList.get(deduplicateList.size() - 1))) {
                deduplicateList.add(currStr);
            }
        }

        return Collections.unmodifiableList(deduplicateList);
    }
}
