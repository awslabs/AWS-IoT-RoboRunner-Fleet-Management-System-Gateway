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

import com.amazon.iotroborunner.fmsg.constants.RoboRunnerWorkerStatusConstants;
import com.amazon.iotroborunner.fmsg.types.WorkerStatus;
import com.amazon.iotroborunner.fmsg.types.mir.MirRobotStatus;

import java.util.Map;

import com.amazonaws.services.iotroborunner.model.CartesianCoordinates;
import com.amazonaws.services.iotroborunner.model.Orientation;
import com.amazonaws.services.iotroborunner.model.PositionCoordinates;
import com.amazonaws.services.iotroborunner.model.VendorProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/** The FmsResponseTranslator used specifically for MiR robot status. */
@Log4j2
public class MirFmsResponseTranslator implements FmsResponseTranslator {
    /** ObjectMapper is used to convert FMS response JSON string to objects. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Translate MiR FMS robot status response to RoboRunner Worker status.
     *
     * @param robotId               The ID of the FMS robot that is being updated in RoboRunner.
     * @param fmsResponse           The string body of the FMS response
     * @param positionTranslator    The PositionTranslation object used to transform FMS reported robot position
     * @param orientationTranslator The OrientationTranslation object used to transform FMS reported robot orientation
     * @return                      RoboRunner Worker Status object built from FMS response
     * @throws JsonProcessingException If there is an issue reading the JSON String
     */
    public WorkerStatus getWorkerStatusFromFmsResponse(
            @NonNull final String robotId,
            @NonNull final String fmsResponse,
            final PositionTranslation positionTranslator,
            final OrientationTranslation orientationTranslator) throws JsonProcessingException {

        final MirRobotStatus mirStatus;
        log.debug("Received FMS response: " + fmsResponse);

        try {
            mirStatus = this.mapper.readValue(fmsResponse, MirRobotStatus.class);
        } catch (JsonProcessingException e) {
            log.error(String.format("Failed to parse FMS response body: %s", e.getMessage()));
            throw e;
        }

        final Map<String, Object> workerAdditionalTransientProperties = Map.of(
            RoboRunnerWorkerStatusConstants.SCHEMA_VERSION_KEY, RoboRunnerWorkerStatusConstants.JSON_SCHEMA_VERSION,
            RoboRunnerWorkerStatusConstants.BATTERY_LEVEL, mirStatus.getBatteryPercentage() / 100.0
        );

        final Map<String, Double> vendorPositionMap = Map.of(
            RoboRunnerWorkerStatusConstants.VENDOR_X, mirStatus.getRobotX(),
            RoboRunnerWorkerStatusConstants.VENDOR_Y, mirStatus.getRobotY()
        );

        final Map<String, Double> vendorOrientationMap = Map.of(
            RoboRunnerWorkerStatusConstants.DEGREES, OrientationTranslation.getPositiveOrientation(
                    mirStatus.getOrientation())
        );

        final Map<String, Object> vendorAdditionalTransientProperties = Map.of(
            RoboRunnerWorkerStatusConstants.SCHEMA_VERSION_KEY, RoboRunnerWorkerStatusConstants.JSON_SCHEMA_VERSION,
            RoboRunnerWorkerStatusConstants.VENDOR_STATE, mirStatus.getState(),
            RoboRunnerWorkerStatusConstants.VENDOR_POSITION, vendorPositionMap,
            RoboRunnerWorkerStatusConstants.VENDOR_ORIENTATION, vendorOrientationMap
        );

        PositionCoordinates position = null;
        Orientation orientation = null;

        if (positionTranslator != null) {
            final double[] rrCoordinates = positionTranslator.getRoboRunnerCoordinatesFromFmsCoordinates(
                mirStatus.getRobotX(), mirStatus.getRobotY());

            position = new PositionCoordinates().withCartesianCoordinates(
                new CartesianCoordinates().withX(rrCoordinates[0]).withY(rrCoordinates[1]));
        }

        if (orientationTranslator != null) {
            final double rrOrientation = orientationTranslator.getRoboRunnerOrientationFromFmsOrientation(
                mirStatus.getOrientation());

            orientation = new Orientation().withDegrees(rrOrientation);
        }

        return WorkerStatus.builder()
            .vendorProperties(new VendorProperties()
                .withVendorWorkerId(robotId)
                .withVendorAdditionalTransientProperties(
                    this.mapper.writeValueAsString(vendorAdditionalTransientProperties)))
            .workerAdditionalTransientProperties(
                this.mapper.writeValueAsString(workerAdditionalTransientProperties))
            .position(position)
            .orientation(orientation)
            .build();
    }
}
