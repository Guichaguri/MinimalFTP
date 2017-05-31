package guichaguri.minimalftp.api;

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
