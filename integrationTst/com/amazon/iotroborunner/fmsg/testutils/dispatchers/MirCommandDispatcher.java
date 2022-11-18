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

package com.amazon.iotroborunner.fmsg.testutils.dispatchers;

import static com.amazon.iotroborunner.fmsg.constants.MirApiEndpointConstants.MISSION_SCHEDULER_ENDPOINT;
import static com.amazon.iotroborunner.fmsg.constants.MirApiRequestParameterConstants.MISSION_DESCRIPTION_PARAM;
import static com.amazon.iotroborunner.fmsg.constants.MirApiRequestParameterConstants.MISSION_EARLIEST_START_TIME_PARAM;
import static com.amazon.iotroborunner.fmsg.constants.MirApiRequestParameterConstants.MISSION_EXECUTION_PRIORITY_HIGH_PARAM;
import static com.amazon.iotroborunner.fmsg.constants.MirApiRequestParameterConstants.MISSION_EXECUTION_PRIORITY_PARAM;
import static com.amazon.iotroborunner.fmsg.constants.MirApiRequestParameterConstants.MISSION_ID_PARAM;
import static com.amazon.iotroborunner.fmsg.constants.MirApiRequestParameterConstants.VENDOR_WORKER_ID_PARAM;
import static com.amazon.iotroborunner.fmsg.constants.MirDateTimeConstants.DATE_TIME_FORMAT;

import com.amazon.iotroborunner.fmsg.clients.MirFmsHttpClient;
import com.amazon.iotroborunner.fmsg.constants.MirApiEndpointConstants;
import com.amazon.iotroborunner.fmsg.testutils.providers.ClientProvider;
import com.amazon.iotroborunner.fmsg.types.FmsHttpRequest;
import com.amazon.iotroborunner.fmsg.types.mir.MirRobotStatus;
import com.amazon.iotroborunner.fmsg.types.mir.MirSharedSpaceEntryStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

/**
 * MirCommandDispatcher class provides all necessary commands
 * for integration tests to interact with MiR FMS.
 */
public class MirCommandDispatcher {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Optional<MirFmsHttpClient> fmsClient = ClientProvider.getMirFmsHttpClient();

    /**
     * Schedules MiR Mission with provided properties.
     *
     * @param missionId the unique identifier of the mission that needs to be executed
     * @param vendorWorkerId the unique identifier of the vendor worker that needs to perform the mission
     * @param desc the description of the mission
     * @return the response received from MiR FMS after scheduling, or throws an exception
     *         if scheduling was unsuccessful
     * @throws RuntimeException if it cannot schedule a MiR mission
     */
    public static String scheduleMirMission(@NonNull final String missionId,
                                            final int vendorWorkerId,
                                            @NonNull final String desc) {
        final Map<String, Object> params = constructScheduleMirMissionParams(missionId, vendorWorkerId, desc);

        final String payload;
        try {
            payload = OBJECT_MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    String.format("Cannot schedule MiR Mission %s. %s", missionId, e.getMessage()));
        }

        return fmsClient.orElseThrow(() -> new RuntimeException("Cannot schedule mission. MiR FMS client is null"))
                .sendFmsRequest(new FmsHttpRequest("POST", MISSION_SCHEDULER_ENDPOINT, payload));
    }

    /**
     * Acquires and returns the MiR vendor worker status by vendor worker id.
     *
     * @param vendorWorkerId the unique identifier of the vendor worker that needs to perform the mission
     * @return the MiR vendor worker resource data if the request was successful or null if
     *         the response cannot be read
     */
    public static MirRobotStatus getVendorWorkerStatusById(final int vendorWorkerId) {
        final String response = fmsClient.get().sendFmsRequest(new FmsHttpRequest(
                "GET", MirApiEndpointConstants.getGetRobotStatusApiEndpoint(vendorWorkerId), ""));
        try {
            return OBJECT_MAPPER.readValue(response, MirRobotStatus.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Returns the shared space data from MiR FMS by given shared space guid.
     *
     * @param sharedSpaceId the unique identifier of the shared space in MiR FMS
     * @return the overall details of the shared space from MiR FMS
     * @throws RuntimeException if it cannot acquire the MiR shared space
     */
    public static String getSharedSpace(@NonNull final String sharedSpaceId) {
        return fmsClient.orElseThrow(() -> new RuntimeException(
                String.format("Cannot get shared space %s from MiR FMS", sharedSpaceId)))
                .sendFmsRequest(new FmsHttpRequest("GET",
                        MirApiEndpointConstants.getSharedSpaceEndpoint(sharedSpaceId), ""));
    }

    /**
     * Blocks or unblocks MiR shared space.
     *
     * @param sharedSpaceId the unique identifier of the shared space
     * @param sharedSpaceEntryStatus the new status of the shared space
     * @return the result of the request to MiR FMS to block or unblock given shared space
     * @throws RuntimeException if it cannot block or unblock the MiR share space
     */
    public static String blockOrUnblockSharedSpace(@NonNull final String sharedSpaceId,
                                                   @NonNull final MirSharedSpaceEntryStatus sharedSpaceEntryStatus) {
        final String blockEndpoint = MirApiEndpointConstants.getBlockedSharedSpaceEndpoint(sharedSpaceId);

        return fmsClient.orElseThrow(() ->
                new RuntimeException(String.format("Shared space %s cannot be %s",
                       sharedSpaceId,  sharedSpaceEntryStatus.name().toLowerCase())))
                .sendFmsRequest(new FmsHttpRequest("PUT", blockEndpoint,
                        String.format("{ \"block\": %b}", sharedSpaceEntryStatus.value)));
    }

    /**
     * Creates the payload for POST request that schedules a mission.
     */
    private static Map<String, Object> constructScheduleMirMissionParams(
            @NonNull final String missionId, final int vendorWorkerId, @NonNull final String desc) {
        return new HashMap() {{
                put(MISSION_ID_PARAM, missionId);
                put(VENDOR_WORKER_ID_PARAM, vendorWorkerId);
                put(MISSION_EARLIEST_START_TIME_PARAM, LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
                put(MISSION_EXECUTION_PRIORITY_PARAM, 0);
                put(MISSION_EXECUTION_PRIORITY_HIGH_PARAM, true);
                put(MISSION_DESCRIPTION_PARAM, desc);
            }};
    }
}
