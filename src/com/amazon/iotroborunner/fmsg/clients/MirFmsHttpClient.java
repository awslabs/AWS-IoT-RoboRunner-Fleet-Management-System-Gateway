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

package com.amazon.iotroborunner.fmsg.clients;

import java.net.http.HttpClient;

import lombok.NonNull;

/**
 * Class containing methods to interact with the MiR Fleet instance.
 */
public final class MirFmsHttpClient extends FmsHttpClient {
    private static final String AUTH_PREFIX = "Basic ";

    /**
     * Constructor for MirFmsHttpClient.
     *
     * @param endpoint   The FMS endpoint
     * @param authSecret The authorization token for the FMS APIs
     */
    public MirFmsHttpClient(@NonNull final String endpoint, @NonNull final String authSecret) {
        super(HttpClient.newHttpClient(), endpoint, AUTH_PREFIX + authSecret);
    }
}
