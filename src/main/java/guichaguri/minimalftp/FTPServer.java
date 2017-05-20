package guichaguri.minimalftp;

import guichaguri.minimalftp.api.IUserAuthenticator;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guilherme Chaguri
 */
public class FTPServer implements Closeable {

    private final boolean multithreaded;

    private ServerSocket socket;
    private IUserAuthenticator auth = null;
    private List<FTPConnection> connections = new ArrayList<FTPConnection>();

    public FTPServer(boolean multithreaded) {
        this.multithreaded = multithreaded;
    }

    public IUserAuthenticator getAuthenticator() {
        if(auth == null) {
            throw new RuntimeException("No authenticator set");
        }
        return auth;
    }

    public void setAuthenticator(IUserAuthenticator auth) {
        this.auth = auth;
    }

    public void listen(int port) throws IOException {
        socket = new ServerSocket(port, 50);
    }

    public void listen(InetAddress address, int port) throws IOException {
        socket = new ServerSocket(port, 50, address);
    }

    public void process() {
        try {
            Socket connection = socket.accept();
            FTPConnection con = new FTPConnection(this, connection);
            Thread thread = new Thread(con);
            thread.start();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public ServerSocket getSocket() {
        return socket;//TODO remove
    }

    @Override
    public void close() throws IOException {
        socket.close();
        socket = null;
    }
}
