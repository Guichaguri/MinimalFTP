/*
 * Copyright 2017 Guilherme Chaguri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guichaguri.minimalftp.api;

import java.io.IOException;

/**
 * This exception can be thrown to send a response to the client.
 * Throwing this exception is the same as calling {@link com.guichaguri.minimalftp.FTPConnection#sendResponse(int, String)}
 * @author Guilherme Chaguri
 */
public class ResponseException extends IOException {

    private final int code;

    public ResponseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
