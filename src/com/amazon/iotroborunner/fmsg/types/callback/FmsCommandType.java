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

package com.amazon.iotroborunner.fmsg.types.callback;

/**
 * Enum containing the different types of commands to FMSs.
 */
public enum FmsCommandType {
    /**
     * Command for getting a robot's status.
     */
    GET_STATUS,
    /**
     * Command for getting a robot's location.
     */
    GET_LOCATION,
    /**
     * Command to listen to all vendor shared spaces.
     */
    LISTEN_TO_VENDOR_SHARED_SPACES,
    /**
     * Command to listen to all vendor shared space exits.
     */
    LISTEN_TO_VENDOR_SHARED_SPACE_EXITS,
    /**
     * Command to request a lock for a Shared Space.
     */
    REQUEST_LOCK_FOR_SHARED_SPACE,
    /**
     * Command to release a lock for a Shared Space.
     */
    REQUEST_RELEASE_LOCK_FOR_SHARED_SPACE,
    /**
     *  Command highlighting a failed attempt to grant access to a Shared Space.
     */
    FAILED_TO_GRANT_ACCESS_TO_SHARED_SPACE,
    /**
     * Command highlighting a failed attempt to block a Shared Space.
     */
    FAILED_TO_BLOCK_SHARED_SPACE
}
