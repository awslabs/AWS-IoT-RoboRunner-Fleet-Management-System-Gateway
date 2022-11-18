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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;
import com.amazon.iotroborunner.fmsg.connectors.MirFmsConnector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the FMS connector factory module. */
@ExtendWith(MockitoExtension.class)
public class FmsConnectorFactoryTest {
    @Mock
    private FmsgConnectorConfiguration mockFleetManagerConfig;

    @Test
    public void given_unsupportedFleetType_when_getFmsConnectorForFleetType_then_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> {
            FmsConnectorFactory.getFmsConnectorForFleetType("MUFFINS", mockFleetManagerConfig);
        });
    }

    @Test
    public void given_mirRobotFleetType_when_getFmsConnectorForFleetType_then_returnsMirFmsConnector() {
        try (MockedConstruction<MirFmsConnector> mocked = mockConstruction(MirFmsConnector.class)) {
            final FmsConnector connector = FmsConnectorFactory.getFmsConnectorForFleetType(
                "MIR", mockFleetManagerConfig
            );

            assertEquals(MirFmsConnector.class, connector.getClass());
        }
    }
}
