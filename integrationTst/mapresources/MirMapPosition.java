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

package mapresources;

import lombok.NonNull;

/**
 * MirMapPosition enum represents the MiR Integration Test map positions used by the tests.
 * The enum stores each position's name, location and orientation exactly as on the map.
 */
public enum MirMapPosition {
    POSITION_A("PositionA", 1.5, 13.5, 0),
    POSITION_B("PositionB", 11.5, 13.5, 180),
    POSITION_C("PositionC", 23.5, 13.5, 180);

    public final String name;
    public final double positionX;
    public final double positionY;
    public final double orientation;

    MirMapPosition(@NonNull final String startPositionName, final double x, final double y, final double o) {
        this.name = startPositionName;
        this.positionX = x;
        this.positionY = y;
        this.orientation = o;
    }
}
