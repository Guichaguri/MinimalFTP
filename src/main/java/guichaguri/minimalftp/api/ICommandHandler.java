package guichaguri.minimalftp.api;

import java.io.IOException;

/**
 * @author Guilherme Chaguri
 */
public interface ICommandHandler {

    void onConnected() throws IOException;

    boolean onCommand(String[] command) throws IOException;

    void onDisconnected() throws IOException;

}
