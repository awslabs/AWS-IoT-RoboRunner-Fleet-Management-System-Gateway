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

package missionresources;

import lombok.NonNull;

/**
 * MirMoveMissionName enum provides a way to store the necessary integration test mission names for MiR FMS.
 * The missions are pre-created and unique identifiers are provided to the integration tests via
 * "integrationTestPropertyConfiguration" file and can be accessed by using the names stored in this enum.
 */
public enum MirMoveMissionName {
    MOVE_TO_POSITION_A("moveToPositionA"),
    MOVE_TO_POSITION_B("moveToPositionB"),
    MOVE_TO_POSITION_C("moveToPositionC");

    public final String name;

    MirMoveMissionName(@NonNull final String name) {
        this.name = name;
    }
}
