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

package com.amazon.iotroborunner.fmsg.workerpropertyupdates;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.iotroborunner.fmsg.connectors.FmsConnector;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the FMSG Worker Property Updates module. */
@ExtendWith(MockitoExtension.class)
public class FmsgWorkerPropertyUpdatesTest {
    @Mock
    private FmsConnector testConnector1;

    @Mock
    private FmsConnector testConnector2;

    @Mock
    private Map<String, FmsConnector> mockConnectors;

    @Test
    public void given_nullConnectors_when_startWorkerPropertyUpdates_then_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            FmsgWorkerPropertyUpdates.startWorkerPropertyUpdates(null);
        });
    }

    @Test
    public void given_listOfConnectors_when_startWorkerPropertyUpdates_then_connectorMethodInvoked() {
        when(mockConnectors.values()).thenReturn(List.of(testConnector1, testConnector2));

        FmsgWorkerPropertyUpdates.startWorkerPropertyUpdates(mockConnectors);

        verify(testConnector1, times(1)).getAllRobotStatuses();
        verify(testConnector2, times(1)).getAllRobotStatuses();
    }
}
