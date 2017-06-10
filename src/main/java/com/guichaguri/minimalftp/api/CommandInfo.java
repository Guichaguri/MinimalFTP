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
 * @author Guilherme Chaguri
 */
public class CommandInfo {

    public final Command command;
    public final String help;
    public final boolean needsAuth;

    public CommandInfo(Command command, String help, boolean needsAuth) {
        this.command = command;
        this.help = help;
        this.needsAuth = needsAuth;
    }

    /**
     * Represents a command
     */
    @FunctionalInterface
    public interface Command {

        /**
         * Runs a command that accepts all arguments
         * @param argument The argument
         * @throws IOException When an error occurs
         */
        void run(String argument) throws IOException;

        default void run(CommandInfo info, String argument) throws IOException {
            if(argument.isEmpty()) throw new ResponseException(501, "Missing parameters");

            run(argument);
        }
    }

    /**
     * Represents a command with no arguments
     */
    @FunctionalInterface
    public interface NoArgsCommand extends Command {

        /**
         * Runs a command that doesn't accept arguments
         * @throws IOException When an error occurs
         */
        void run() throws IOException;

        @Override
        default void run(String argument) throws IOException {
            run();
        }

        @Override
        default void run(CommandInfo info, String argument) throws IOException {
            run();
        }

    }

    /**
     * Represents a command with an array of arguments
     */
    @FunctionalInterface
    public interface ArgsArrayCommand extends Command {

        /**
         * Runs a command that accepts only a single argument
         * @param argument An array of arguments
         * @throws IOException When an error occurs
         */
        void run(String[] argument) throws IOException;

        @Override
        default void run(String argument) throws IOException {
            run(argument.split("\\s+"));
        }

        @Override
        default void run(CommandInfo info, String argument) throws IOException {
            run(argument.split("\\s+"));
        }

    }
}
