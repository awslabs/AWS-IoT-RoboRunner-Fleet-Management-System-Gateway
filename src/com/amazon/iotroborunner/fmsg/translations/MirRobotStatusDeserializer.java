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

import com.amazon.iotroborunner.fmsg.types.mir.MirRobotStatus;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/** Class that deserializes FMS response JSON string into MirRobotStstus objects. */
public class MirRobotStatusDeserializer extends StdDeserializer<MirRobotStatus> {
    private static final String STATUS = "status";
    private static final String BATTERY_PERCENTAGE = "battery_percentage";
    private static final String POSITION = "position";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String ORIENTATION = "orientation";
    private static final String STATE_TEXT = "state_text";

    /**
     * Constructor with no arguments.
     * Assumes MirRobotStatus deserializer.
     */
    public MirRobotStatusDeserializer() {
        this(null);
    }

    /**
     * Constructor with class argument.
     *
     * @param vc The class to desieralize to
     */
    public MirRobotStatusDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public MirRobotStatus deserialize(final JsonParser jp, final DeserializationContext ctxt) throws
            IOException, JsonProcessingException {
        final JsonNode fmsResponseNode = jp.getCodec().readTree(jp);
        final double batteryPercentage = fmsResponseNode.get(STATUS).get(BATTERY_PERCENTAGE).asDouble();
        final double x = fmsResponseNode.get(STATUS).get(POSITION).get(X).asDouble();
        final double y = fmsResponseNode.get(STATUS).get(POSITION).get(Y).asDouble();
        final double orientation = fmsResponseNode.get(STATUS).get(POSITION).get(ORIENTATION).asDouble();
        final String state = fmsResponseNode.get(STATUS).get(STATE_TEXT).asText();

        final MirRobotStatus status = new MirRobotStatus();
        status.setBatteryPercentage(batteryPercentage);
        status.setRobotX(x);
        status.setRobotY(y);
        status.setOrientation(orientation);
        status.setState(state);

        return status;
    }
}
