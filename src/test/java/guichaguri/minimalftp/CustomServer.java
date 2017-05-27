package guichaguri.minimalftp;

import guichaguri.minimalftp.custom.UserbaseAuthenticator;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Guilherme Chaguri
 */
public class CustomServer {

    public static void main(String[] args) throws IOException {
        // Create the FTP server
        FTPServer server = new FTPServer();

        // Create our custom authenticator
        UserbaseAuthenticator auth = new UserbaseAuthenticator();

        // Register a few users
        auth.registerUser("john", "1234");
        auth.registerUser("alex", "abcd123");
        auth.registerUser("hannah", "98765");

        // Set our custom authenticator
        server.setAuthenticator(auth);

        // Start it synchronously in our localhost and in the port 21
        server.listenSync(InetAddress.getByName("localhost"), 21);
    }

}
