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

import static com.amazon.iotroborunner.fmsg.types.mir.MirSharedSpaceEntryStatus.UNBLOCKED;
import static java.util.Map.Entry;
import static mapresources.MirMapPosition.POSITION_A;
import static mapresources.MirMapPosition.POSITION_B;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.testexecutioncontroller.RunTestIfApplicationEnabled;
import com.amazon.iotroborunner.fmsg.testexecutioncontroller.testgroupannotations.WpuIntegrationTest;
import com.amazon.iotroborunner.fmsg.testutils.dispatchers.MirCommandDispatcher;
import com.amazon.iotroborunner.fmsg.testutils.helpers.IotRoboRunnerTestHelpers;
import com.amazon.iotroborunner.fmsg.testutils.helpers.MirTestHelpers;
import com.amazon.iotroborunner.fmsg.testutils.providers.ConfigurationProvider;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iotroborunner.model.CartesianCoordinates;
import com.amazonaws.services.iotroborunner.model.GetWorkerResult;
import com.amazonaws.services.iotroborunner.model.PositionCoordinates;
import com.amazonaws.services.iotroborunner.model.VendorProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import missionresources.MirMoveMissionName;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

/**
 * WorkerPropertyUpdatesMirIntegrationTest class tests the functionality of
 * Worker Property Updates application end to end for MiR vendor worker.
 */
@WpuIntegrationTest
@RunTestIfApplicationEnabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkerPropertyUpdatesMirIntegrationTest {
    private static final FmsgCoreConfiguration fmsgCoreConfig = ConfigurationProvider.getIotRoboRunnerConfig();
    final Map<String, FmsgConnectorConfiguration> fmsgConnectorConfig = ConfigurationProvider.getFmsgConfigs();

    /**
     *  Vendor worker id to its corresponding IoT RoboRunner worker arn mapping.
     */
    private Pair<Integer, String> vendorWorkerIdToWorkerArnMapping;


    /**
     * Initialize needed resource(s) before all the tests get to run.
     */
    @BeforeAll
    public void initialize() throws JsonProcessingException, InvalidPropertiesFormatException {
        this.vendorWorkerIdToWorkerArnMapping = IotRoboRunnerTestHelpers.getVendorWorkerIdToWorkerArnMapping(
                fmsgCoreConfig.getSiteArn(), fmsgConnectorConfig.get(RobotFleetType.MIR.value).getWorkerFleetArn());

        // WPU tests do not use the shared space on tha map, therefore, we will unblock the shared space in MiR FMS
        // to make sure the MiR worker does not encounter issues when left in an unexpected location on the map.
        final String sharedSpaceId = IotRoboRunnerTestHelpers.getSharedSpaceArnToVendorSharedSpaceIdMapping(
                fmsgCoreConfig.getSiteArn(),
                fmsgConnectorConfig.get(RobotFleetType.MIR.value).getWorkerFleetArn()).getValue();
        MirCommandDispatcher.blockOrUnblockSharedSpace(sharedSpaceId, UNBLOCKED);
    }

    /**
     * Sets up the vendor worker in an initial position from where the test(s) can start.
     */
    @BeforeEach
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    public void setup() throws InterruptedException {
        MirTestHelpers.executeMoveMissionUntilCompleteBasedOnIotRoboRunnerWorkerState(
                MirMoveMissionName.MOVE_TO_POSITION_A.name, vendorWorkerIdToWorkerArnMapping, POSITION_A, false);
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void given_aVendorWorkerAtStartingPositionOnMirMap_when_vendorWorkerMovesFromPositionToPosition_then_workerVendorPropertiesSyncedToIotRoboRunnerContinuously()
            throws JsonProcessingException, InterruptedException {

        final NavigableMap<String, GetWorkerResult> workerUpdateHistory =
                MirTestHelpers.executeMoveMissionUntilCompleteBasedOnIotRoboRunnerWorkerState(
                        MirMoveMissionName.MOVE_TO_POSITION_B.name,
                        vendorWorkerIdToWorkerArnMapping, POSITION_B, true);

        final NavigableMap<String, VendorProperties> workerVendorPropertiesHistory =
                IotRoboRunnerTestHelpers.getUpdatedTimeToWorkerVendorPropertiesMap(workerUpdateHistory);

        assertTrue(workerVendorPropertiesHistory.size() > 0);

        double previousDistance = 0.0;
        final List<String> workerVendorStateUpdateHistory = new ArrayList<>();

        for (Entry<String, VendorProperties> currVendorPropUpdate : workerVendorPropertiesHistory.entrySet()) {
            final Entry<String, VendorProperties> nextVendorPropUpdate = workerVendorPropertiesHistory
                    .higherEntry(currVendorPropUpdate.getKey());

            final PositionCoordinates currPosition = IotRoboRunnerTestHelpers.getVendorWorkerPosition(
                    currVendorPropUpdate.getValue());

            final String currState = IotRoboRunnerTestHelpers.getVendorWorkerState(currVendorPropUpdate.getValue());
            workerVendorStateUpdateHistory.add(currState);

            if (nextVendorPropUpdate == null) {
                IotRoboRunnerTestHelpers.validateVendorWorkerFinalPositionInIotRoboRunner(
                        currPosition, new PositionCoordinates().withCartesianCoordinates(
                                new CartesianCoordinates()
                                        .withX(POSITION_B.positionX)
                                        .withY(POSITION_B.positionY)
                                        .withZ(0.0)));
                break;
            }

            final String nextState = IotRoboRunnerTestHelpers.getVendorWorkerState(nextVendorPropUpdate.getValue());
            final PositionCoordinates nextPosition = IotRoboRunnerTestHelpers.getVendorWorkerPosition(
                    nextVendorPropUpdate.getValue());

            final double dist = IotRoboRunnerTestHelpers.validateVendorWorkerMoveUpdatesInIotRoboRunner(
                    currPosition, nextPosition, currState, nextState, previousDistance);

            previousDistance += dist;
        }

        IotRoboRunnerTestHelpers.validateVendorWorkerStateUpdatesInIotRoboRunner(workerVendorStateUpdateHistory);
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void given_aVendorWorkerAtStartingPositionOnMirMap_when_vendorWorkerMovesFromPositionToPosition_then_updateFrequencyIsWithinAllowedRange()
            throws InterruptedException {
        final NavigableMap<String, GetWorkerResult> workerUpdateHistory =
                MirTestHelpers.executeMoveMissionUntilCompleteBasedOnIotRoboRunnerWorkerState(
                        MirMoveMissionName.MOVE_TO_POSITION_B.name, vendorWorkerIdToWorkerArnMapping, POSITION_B, true);

        final List<Duration> durationsBetweenWorkerUpdates = IotRoboRunnerTestHelpers.getDurationBetweenWorkerUpdates(
                new ArrayList<>(workerUpdateHistory.values()));

        assertNotNull(durationsBetweenWorkerUpdates);

        IotRoboRunnerTestHelpers.validateDurationBetweenWorkerUpdatesIsInAllowedRange(durationsBetweenWorkerUpdates);
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void given_aVendorWorkerAtStartingPositionOnMirMap_when_vendorWorkerMovesFromPositionToPosition_then_workerOrientationSyncedToIotRoboRunnerContinuously()
            throws JsonProcessingException, InterruptedException {
        // Using stop watch to measure the execution time of the mission. It will be used to infer approximately
        // how many times the orientation of the worker resource should have been updated.
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final NavigableMap<String, GetWorkerResult> workerUpdateHistory =
                MirTestHelpers.executeMoveMissionUntilCompleteBasedOnIotRoboRunnerWorkerState(
                        MirMoveMissionName.MOVE_TO_POSITION_B.name,
                        vendorWorkerIdToWorkerArnMapping, POSITION_B, true);

        stopWatch.stop();

        final List<Double> orientationValuesDuringWorkerUpdates = IotRoboRunnerTestHelpers.getOrientationTimeSeries(
                new ArrayList<>(workerUpdateHistory.values()));

        assertNotNull(orientationValuesDuringWorkerUpdates);

        final long missionTotalTimeInSeconds = stopWatch.getTime() / 1000;
        IotRoboRunnerTestHelpers.validateOrientationChangesBetweenWorkerUpdates(
                orientationValuesDuringWorkerUpdates, missionTotalTimeInSeconds);
        IotRoboRunnerTestHelpers.validateFinalOrientationChangeAtDestination(
                workerUpdateHistory.lastEntry().getValue().getVendorProperties(), POSITION_B.orientation);
    }
}
