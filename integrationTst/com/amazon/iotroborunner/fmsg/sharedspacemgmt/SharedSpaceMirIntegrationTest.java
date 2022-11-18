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

package com.amazon.iotroborunner.fmsg.sharedspacemgmt;

import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerCustomPropertyKey.WORKER_LOCATION_STATUS;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.IN_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.OUT_OF_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.constants.SimulatedFmsWorkerLocationStatus.WAITING_FOR_SHARED_SPACE;
import static com.amazon.iotroborunner.fmsg.testutils.helpers.IotRoboRunnerTestHelpers.getVendorWorkerIdToWorkerArnMapping;
import static com.amazon.iotroborunner.fmsg.testutils.helpers.IotRoboRunnerTestHelpers.getWorker;
import static com.amazon.iotroborunner.fmsg.types.mir.MirSharedSpaceEntryStatus.BLOCKED;
import static com.amazon.iotroborunner.fmsg.types.mir.MirSharedSpaceEntryStatus.UNBLOCKED;
import static mapresources.MirMapPosition.POSITION_B;
import static mapresources.MirMapPosition.POSITION_C;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.iotroborunner.fmsg.config.FmsgConnectorConfiguration;
import com.amazon.iotroborunner.fmsg.config.FmsgCoreConfiguration;
import com.amazon.iotroborunner.fmsg.testexecutioncontroller.RunTestIfApplicationEnabled;
import com.amazon.iotroborunner.fmsg.testexecutioncontroller.testgroupannotations.SmIntegrationTest;
import com.amazon.iotroborunner.fmsg.testutils.dispatchers.MirCommandDispatcher;
import com.amazon.iotroborunner.fmsg.testutils.helpers.IotRoboRunnerTestHelpers;
import com.amazon.iotroborunner.fmsg.testutils.helpers.MirTestHelpers;
import com.amazon.iotroborunner.fmsg.testutils.providers.ConfigurationProvider;
import com.amazon.iotroborunner.fmsg.types.RobotFleetType;
import com.amazon.iotroborunner.fmsg.types.mir.MirRobotStatus;

import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iotroborunner.model.DestinationState;
import com.amazonaws.services.iotroborunner.model.GetWorkerResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import missionresources.MirMoveMissionName;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.locationtech.jts.geom.Coordinate;

/**
 * SharedSpaceMirIntegrationTest class tests the functionality of
 * Shared Space Management (SM) sample application end to end for MiR
 * vendor worker and a secondary simulated vendor worker.
 */
@SmIntegrationTest
@RunTestIfApplicationEnabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SharedSpaceMirIntegrationTest {
    private static final int MIR_WORKER_SHARED_SPACE_MAX_CROSSING_TIME_IN_MS = 30000;
    private static final int SIMULATED_WORKER_SHARED_SPACE_MAX_CROSSING_TIME_IN_MS = 15000;
    private static final int SCHEDULED_TASK_DELAY_IN_SECONDS = 5;

    private static final FmsgCoreConfiguration fmsgCoreConfig = ConfigurationProvider.getIotRoboRunnerConfig();
    final Map<String, FmsgConnectorConfiguration> fmsgConnectorConfig = ConfigurationProvider.getFmsgConfigs();

    /**
     *  Vendor worker id to its corresponding IoT RoboRunner worker arn mapping.
     */
    private Pair<Integer, String> mirVendorWorkerIdToWorkerArnMapping;

    /**
     * Simulated FMS vendor worker arn in IoT RoboRunner.
     */
    private String simulatedWorkerArn;

    /**
     * Shared space used for testing the shared space management
     * between MiR FMS vendor worker and Simulated FMS worker.
     */
    private Pair<String, String> sharedSpaceArnToSharedSpaceId;

    /**
     * Initializes needed resources before all the tests are run.
     */
    @BeforeAll
    public void initialize() throws InvalidPropertiesFormatException, JsonProcessingException {
        this.mirVendorWorkerIdToWorkerArnMapping = getVendorWorkerIdToWorkerArnMapping(fmsgCoreConfig.getSiteArn(),
                fmsgConnectorConfig.get(RobotFleetType.MIR.value).getWorkerFleetArn());

        this.sharedSpaceArnToSharedSpaceId = IotRoboRunnerTestHelpers.getSharedSpaceArnToVendorSharedSpaceIdMapping(
                fmsgCoreConfig.getSiteArn(),
                fmsgConnectorConfig.get(RobotFleetType.MIR.value).getWorkerFleetArn());

        this.simulatedWorkerArn = getWorker(fmsgCoreConfig.getSiteArn(),
                fmsgConnectorConfig.get(RobotFleetType.SIMULATED.value).getWorkerFleetArn()).getArn();
    }

    /**
     * Moves MiR vendor worker to the initial position that is expected by test cases.
     */
    @BeforeEach
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    public void setup() throws InterruptedException {
        // Unblock the shared space as this is not a test and the worker will need to get to
        // its initial destination to start the tests.
        MirCommandDispatcher.blockOrUnblockSharedSpace(sharedSpaceArnToSharedSpaceId.getValue(), UNBLOCKED);

        MirTestHelpers.executeMoveMissionUntilCompleteBasedOnVendorWorkerStatus(
                MirMoveMissionName.MOVE_TO_POSITION_B.name,
                mirVendorWorkerIdToWorkerArnMapping.getKey(), POSITION_B);

        // Block the shared space as the control over the shared space during testing should
        // be managed via SM sample application.
        MirCommandDispatcher.blockOrUnblockSharedSpace(sharedSpaceArnToSharedSpaceId.getValue(), BLOCKED);
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void given_mirAndSimulatedWorker_when_bothMoveToCrossSharedSpace_then_simulatedWorkerCrossesThenMirWorker()
            throws InterruptedException, JsonProcessingException, ExecutionException {
        final MirRobotStatus mirWorkerInitialStatus = MirCommandDispatcher.getVendorWorkerStatusById(
                mirVendorWorkerIdToWorkerArnMapping.getKey());

        // Simulated worker asks for the shared space first.
        IotRoboRunnerTestHelpers.updateWorker(simulatedWorkerArn,
                sharedSpaceArnToSharedSpaceId.getKey(), WAITING_FOR_SHARED_SPACE.value);

        // Scheduling MiR vendor worker to request the lock to the shared space after simulated worker.
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<Boolean> scheduledFuture = executor.schedule(() -> {
            try {
                MirTestHelpers.executeMoveMissionUntilCompleteBasedOnVendorWorkerStatus(
                        MirMoveMissionName.MOVE_TO_POSITION_C.name,
                        mirVendorWorkerIdToWorkerArnMapping.getKey(), POSITION_C);
            } catch (InterruptedException e) {
                return false;
            }
            return true;
        }, SCHEDULED_TASK_DELAY_IN_SECONDS, TimeUnit.SECONDS);

        // The timeout is chosen specifically based on the map setup that MiR FMS has for integration
        // testing, to make sure MiR vendor worker gets to the shared space, requests the lock, and waits there
        // for some time before the lock is released by the simulated worker. If the map positions or shared
        // space locations are adjusted in MiR FMS, the timeout should be adjusted as well.
        Thread.sleep(MIR_WORKER_SHARED_SPACE_MAX_CROSSING_TIME_IN_MS);

        if (scheduledFuture.getDelay(TimeUnit.SECONDS) > 0) {
            Thread.sleep(MIR_WORKER_SHARED_SPACE_MAX_CROSSING_TIME_IN_MS);
        }

        final MirRobotStatus mirWorkerStatusBeforeSimulatedWorkerReleasesLock = MirCommandDispatcher
                .getVendorWorkerStatusById(mirVendorWorkerIdToWorkerArnMapping.getKey());

        IotRoboRunnerTestHelpers.updateWorker(simulatedWorkerArn,
                sharedSpaceArnToSharedSpaceId.getKey(), OUT_OF_SHARED_SPACE.value);

        while (!scheduledFuture.isDone()) {
            Thread.sleep(3000);
        }

        final MirRobotStatus mirWorkerStatusWhenMissionFinished = MirCommandDispatcher.getVendorWorkerStatusById(
                mirVendorWorkerIdToWorkerArnMapping.getKey());

        // Check that MiR vendor worker was at its expected initial position before the mission started.
        assertTrue(MirTestHelpers.isVendorWorkerAtGivenPosition(
                new Coordinate(mirWorkerInitialStatus.getRobotX(), mirWorkerInitialStatus.getRobotY()),
                new Coordinate(POSITION_B.positionX, POSITION_B.positionY)));

        // Check that the execution for MiR vendor worker in a separate thread
        // successfully happened and no error was thrown.
        assertTrue(scheduledFuture.get().booleanValue());

        // Check if the order of entering shared space was as planned (simulated worker first, then MiR vendor worker).
        final boolean wasMirWorkerInSharedSpaceBeforeSimulatedWorker = MirTestHelpers.isCoordinateInSharedSpace(
                sharedSpaceArnToSharedSpaceId.getValue(),
                new Coordinate(
                        mirWorkerStatusBeforeSimulatedWorkerReleasesLock.getRobotX(),
                        mirWorkerStatusBeforeSimulatedWorkerReleasesLock.getRobotY()));
        assertFalse(wasMirWorkerInSharedSpaceBeforeSimulatedWorker);

        // When waiting for the lock, checking that while the MiR vendor worker was outside
        // the shared space, it was waiting on its path to its destination before entering
        // the shared space and not after crossing it.
        assertTrue(MirTestHelpers.didVendorWorkerEnterSharedSpace(sharedSpaceArnToSharedSpaceId.getValue(),
                mirWorkerStatusBeforeSimulatedWorkerReleasesLock.getRobotX()));

        // Additional check to confirm the MiR vendor worker moved from initial location and stopped at shared space.
        assertTrue(MirTestHelpers.hasVendorWorkerMovedSinceLastCheck(
                mirWorkerInitialStatus, mirWorkerStatusBeforeSimulatedWorkerReleasesLock));

        // Check that MiR vendor worker made it to its destination after all.
        assertTrue(MirTestHelpers.isVendorWorkerAtGivenPosition(
                new Coordinate(mirWorkerStatusWhenMissionFinished.getRobotX(),
                        mirWorkerStatusWhenMissionFinished.getRobotY()),
                new Coordinate(POSITION_C.positionX, POSITION_C.positionY)));
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void given_mirAndSimulatedWorker_when_bothMoveToCrossSharedSpace_then_mirVendorWorkerCrossesThenSimulatedWorker()
            throws InterruptedException, JsonProcessingException {
        // Scheduling MiR vendor worker to request the lock before the simulated worker does.
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<Boolean> scheduledFuture = executor.schedule(() -> {
            try {
                MirTestHelpers.executeMoveMissionUntilCompleteBasedOnVendorWorkerStatus(
                        MirMoveMissionName.MOVE_TO_POSITION_C.name,
                        mirVendorWorkerIdToWorkerArnMapping.getKey(), POSITION_C);
            } catch (InterruptedException e) {
                return false;
            }
            return true;
        }, 0, TimeUnit.SECONDS);

        if (scheduledFuture.getDelay(TimeUnit.SECONDS) > 0) {
            Thread.sleep(fmsgCoreConfig.getVendorSharedSpacePollingInterval() * 1000 * 2);
        }

        final List<GetWorkerResult> simulatedWorkerUpdateHistory = new ArrayList<>();
        GetWorkerResult simulatedWorkerFinalUpdate = null;
        boolean didSimulateWorkerRequestLock = false;

        while (!scheduledFuture.isDone()) {
            final MirRobotStatus mirWorkerStatus = MirCommandDispatcher
                    .getVendorWorkerStatusById(mirVendorWorkerIdToWorkerArnMapping.getKey());
            final boolean isMirWorkerInSharedSpace = MirTestHelpers.isCoordinateInSharedSpace(
                    sharedSpaceArnToSharedSpaceId.getValue(), new Coordinate(
                            mirWorkerStatus.getRobotX(),
                            mirWorkerStatus.getRobotY()));

            // If MiR vendor worker is in shared space, we can request a lock for simulated worker to later validate
            // that the simulated worker waited until MiR vendor worker left the shared space.
            if (isMirWorkerInSharedSpace) {
                if (!didSimulateWorkerRequestLock) {
                    IotRoboRunnerTestHelpers.updateWorker(simulatedWorkerArn,
                            sharedSpaceArnToSharedSpaceId.getKey(), WAITING_FOR_SHARED_SPACE.value);
                    didSimulateWorkerRequestLock = true;
                } else {
                    simulatedWorkerUpdateHistory.add(IotRoboRunnerTestHelpers.getWorkerById(simulatedWorkerArn));
                }

            }
            Thread.sleep(1000);
        }

        if (didSimulateWorkerRequestLock) {
            Thread.sleep(fmsgCoreConfig.getVendorSharedSpacePollingInterval() * 1000 * 2);
            simulatedWorkerFinalUpdate = IotRoboRunnerTestHelpers.getWorkerById(simulatedWorkerArn);
        }

        final MirRobotStatus mirWorkerStatusWhenMissionFinished = MirCommandDispatcher.getVendorWorkerStatusById(
                mirVendorWorkerIdToWorkerArnMapping.getKey());

        // Request to release the lock that was acquired for the simulated worker (clean up).
        IotRoboRunnerTestHelpers.updateWorker(simulatedWorkerArn,
                sharedSpaceArnToSharedSpaceId.getKey(), OUT_OF_SHARED_SPACE.value);

        assertTrue(didSimulateWorkerRequestLock);
        assertFalse(simulatedWorkerUpdateHistory.isEmpty());
        assertNotNull(simulatedWorkerFinalUpdate);

        // Check that MiR vendor worker made it to its destination after all.
        assertTrue(MirTestHelpers.isVendorWorkerAtGivenPosition(
                new Coordinate(mirWorkerStatusWhenMissionFinished.getRobotX(),
                        mirWorkerStatusWhenMissionFinished.getRobotY()),
                new Coordinate(POSITION_C.positionX, POSITION_C.positionY)));

        // While MiR vendor worker was in shared space, confirm that the
        // simulated worker did not enter the shared space.
        for (final GetWorkerResult workerResult : simulatedWorkerUpdateHistory) {
            final Map<String, String> customTransientProperties = IotRoboRunnerTestHelpers
                    .getWorkerAdditionalTransientCustomProperties(workerResult.getAdditionalTransientProperties());
            assertEquals(WAITING_FOR_SHARED_SPACE.value, customTransientProperties.get(WORKER_LOCATION_STATUS.value));
        }

        // After the shared space was freed from MiR vendor worker, the simulated worker
        // should indicate that it is in the shared space.
        final Map<String, String> finalCustomTransientPropertiesOfWorker = IotRoboRunnerTestHelpers
                .getWorkerAdditionalTransientCustomProperties(simulatedWorkerFinalUpdate
                        .getAdditionalTransientProperties());
        assertEquals(IN_SHARED_SPACE.value, finalCustomTransientPropertiesOfWorker.get(WORKER_LOCATION_STATUS.value));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void given_simulatedWorker_when_acquiringLockToSharedSpace_then_fmsgDisablesSharedSpaceInIotRoboRunner()
            throws JsonProcessingException, InterruptedException {

        // Initial state of the shared space.
        final String sharedSpaceStateBeforeSimulatedVendorWorker = IotRoboRunnerTestHelpers.getDestinationById(
                sharedSpaceArnToSharedSpaceId.getKey()).getState();

        // Simulated worker requests the lock to the shared space.
        IotRoboRunnerTestHelpers.updateWorker(simulatedWorkerArn,
                sharedSpaceArnToSharedSpaceId.getKey(), WAITING_FOR_SHARED_SPACE.value);

        Thread.sleep(SIMULATED_WORKER_SHARED_SPACE_MAX_CROSSING_TIME_IN_MS);

        // State of shared space when it is acquired.
        final String sharedSpaceStateAfterSharedSpaceLockAcquired = IotRoboRunnerTestHelpers.getDestinationById(
                sharedSpaceArnToSharedSpaceId.getKey()).getState();

        // Simulated worker requests to release the lock to the shared space.
        IotRoboRunnerTestHelpers.updateWorker(simulatedWorkerArn,
                sharedSpaceArnToSharedSpaceId.getKey(), OUT_OF_SHARED_SPACE.value);

        Thread.sleep(fmsgCoreConfig.getVendorSharedSpacePollingInterval() * 1000 * 2);

        // The state of the shared space when simulated worker is out of it.
        final String sharedSpaceFinalState = IotRoboRunnerTestHelpers.getDestinationById(
                sharedSpaceArnToSharedSpaceId.getKey()).getState();

        assertEquals(DestinationState.ENABLED.toString(), sharedSpaceStateBeforeSimulatedVendorWorker);
        assertEquals(DestinationState.DISABLED.toString(), sharedSpaceStateAfterSharedSpaceLockAcquired);
        assertEquals(DestinationState.ENABLED.toString(), sharedSpaceFinalState);
    }
}
