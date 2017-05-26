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

        void run(String[] args) throws IOException;

    }

    /**
     * Represents a command with no arguments
     */
    @FunctionalInterface
    public interface NoArgsCommand extends Command {

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

        void run(String argument) throws IOException;

        default void run(String[] args) throws IOException {
            if(args.length < 2) throw new ResponseException(501, "Missing parameters");

            run(args[1]);
        }

    }
}
