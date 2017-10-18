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

import com.guichaguri.minimalftp.FTPConnection;

/**
 * Represents an user authenticator.
 * You can implement your existing user database
 *
 * @author Guilherme Chaguri
 */
public interface IUserAuthenticator {

    /**
     * Whether this authenticator requires a username.
     *
     * @param con The FTP connection
     * @return {@code true} if this authenticator requires a username
     */
    boolean needsUsername(FTPConnection con);

    /**
     * Whether this authenticator requires a password.
     *
     * Only affects when {@link #needsUsername(FTPConnection)} is also {@code true}
     *
     * @param con The FTP connection
     * @param username The username
     * @return {@code true} if this authenticator requires a password
     */
    boolean needsPassword(FTPConnection con, String username);

    /**
     * Authenticates a user synchronously.
     *
     * You use a custom file system depending on the user
     *
     * @param con The FTP connection
     * @param username The username or {@code null} when {@link #needsUsername(FTPConnection)} returns false
     * @param password The password or {@code null} when {@link #needsPassword(FTPConnection, String)} returns false
     * @return A file system if the authentication succeeded
     * @throws AuthException When the authentication failed
     */
    IFileSystem authenticate(FTPConnection con, String username, String password) throws AuthException;


    /**
     * The exception that should be thrown when the authentication fails
     */
    class AuthException extends RuntimeException { }

}
