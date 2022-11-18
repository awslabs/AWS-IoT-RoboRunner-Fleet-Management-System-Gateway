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

package com.amazon.iotroborunner.fmsg;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;
import com.amazon.iotroborunner.fmsg.connectors.MirFmsConnector;
import com.amazon.iotroborunner.fmsg.connectors.simulatedconnector.SimulatedFmsConnector;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;


/**
 * Class for getting the FMS associated objects from the fleet type.
 */
public final class FmsConnectorFactory {
    private static final Map<RobotFleetType, Function<FmsgConnectorConfiguration, FmsConnector>>
        FMS_CONNECTOR_CONSTRUCTORS = Map.ofEntries(
            Map.entry(RobotFleetType.MIR, MirFmsConnector::new),
            Map.entry(RobotFleetType.SIMULATED, SimulatedFmsConnector::new)
        );

    /**
     * Don't allow object instantiation.
     */
    private FmsConnectorFactory() {
    }

    /**
     * Get the FMS connector by the type read from the config.
     *
     * @param type                 The FMS type
     * @param fleetManagerConfig   The fleet manager configuration for the FMS type
     * @return The instantiated FMS connector
     * @throws RuntimeException If the client type is not currently supported
     */
    public static FmsConnector getFmsConnectorForFleetType(
            @NonNull final String type, @NonNull final FmsgConnectorConfiguration fleetManagerConfig)
                    throws RuntimeException {

        return Arrays.stream(RobotFleetType.values())
            .filter(robotFleetType -> StringUtils.equalsIgnoreCase(robotFleetType.value, type))
            .map(FMS_CONNECTOR_CONSTRUCTORS::get)
            .filter(Objects::nonNull)
            .map(constructor -> constructor.apply(fleetManagerConfig))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Unsupported fleet type: " + type));
    }
}
