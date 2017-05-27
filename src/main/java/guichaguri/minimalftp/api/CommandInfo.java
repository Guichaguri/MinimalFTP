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
         * @param args An array with the command label and the arguments
         * @throws IOException When an error occurs
         */
        void run(String[] args) throws IOException;

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

        default void run(String[] args) throws IOException {
            run();
        }

    }

    /**
     * Represents a command with only one argument
     */
    @FunctionalInterface
    public interface SingleArgCommand extends Command {

        /**
         * Runs a command that accepts only a single argument
         * @param argument The argument
         * @throws IOException When an error occurs
         */
        void run(String argument) throws IOException;

        default void run(String[] args) throws IOException {
            if(args.length < 2) throw new ResponseException(501, "Missing parameters");

            run(args[1]);
        }

    }
}
