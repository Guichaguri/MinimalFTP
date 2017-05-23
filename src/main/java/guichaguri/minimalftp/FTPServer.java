package guichaguri.minimalftp;

import guichaguri.minimalftp.api.IUserAuthenticator;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FTP Server
 * @author Guilherme Chaguri
 */
public class FTPServer implements Closeable {

    protected final List<FTPConnection> connections = Collections.synchronizedList(new ArrayList<FTPConnection>());

    protected IUserAuthenticator auth = null;

    protected ServerSocket socket = null;
    protected ServerThread serverThread = null;

    /**
     * Creates a new server
     */
    public FTPServer() {

    }

    /**
     * Creates a new server
     * @param auth An authenticator
     */
    public FTPServer(IUserAuthenticator auth) {
        setAuthenticator(auth);
    }

    /**
     * Gets the server address
     * @return The server address or {@code null} if the server is not running
     */
    public InetAddress getAddress() {
        return socket != null ? socket.getInetAddress() : null;
    }

    /**
     * Gets the server port
     * @return The server port or {@code -1} if the server is not running
     */
    public int getPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }

    /**
     * Gets the current authenticator instance.
     * @return The authenticator
     */
    public IUserAuthenticator getAuthenticator() {
        return auth;
    }

    /**
     * Sets the authenticator instance.
     * Not only you can have your own user database, but you can also
     * provide a different file system depending on the user.
     *
     * @param auth The authenticator
     * @see guichaguri.minimalftp.impl.NoOpAuthenticator
     */
    public void setAuthenticator(IUserAuthenticator auth) {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        this.auth = auth;
    }

    /**
     * Starts the FTP server asynchronously
     *
     * @param port The server port
     * @throws IOException
     */
    public void listen(int port) throws IOException {
        listen(null, port);
    }

    /**
     * Starts the FTP server asynchronously
     *
     * @param address The server address or {@code null} for a local address
     * @param port The server port or {@code 0} to automatically allocate the port
     * @throws IOException
     */
    public void listen(InetAddress address, int port) throws IOException {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        if(socket != null) throw new IOException("Server already started");

        socket = new ServerSocket(port, 50, address);

        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * Starts the FTP server synchronously
     * It will block the current thread
     * Connections to the server will still create new threads
     *
     * @param port The server port
     * @throws IOException
     */
    public void listenSync(int port) throws IOException {
        listenSync(null, port);
    }

    /**
     * Starts the FTP server synchronously
     * It will block the current thread
     * Connections to the server will still create new threads
     *
     * @param address The server address or {@code null} for a local address
     * @param port The server port or {@code 0} to automatically allocate the port
     * @throws IOException
     */
    public void listenSync(InetAddress address, int port) throws IOException {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        if(socket != null) throw new IOException("Server already started");

        socket = new ServerSocket(port, 50, address);

        while(!socket.isClosed()) {
            update();
        }
    }

    /**
     * Updates the server
     */
    protected void update() {
        try {
            Socket connection = socket.accept();
            addConnection(new FTPConnection(this, connection));
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Called when a connection is created
     */
    protected void addConnection(FTPConnection con) {
        System.out.println("New connection: " + con.getAddress().toString());//TODO remove debug

        synchronized(connections) {
            connections.add(con);
        }
    }

    /**
     * Called when a connection is terminated
     */
    protected void removeConnection(FTPConnection con) {
        System.out.println("Removing connection " + con.getAddress().toString());//TODO remove debug

        synchronized(connections) {
            connections.remove(con);
        }
    }

    /**
     * Starts disposing server resources
     */
    protected void stop() {
        if(serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }

        for(FTPConnection con : connections) {
            Utils.closeQuietly(con);
        }
    }

    @Override
    public void close() throws IOException {
        stop();

        if(socket != null) {
            socket.close();
            socket = null;
        }
    }

    /**
     * Thread that processes this server when listening asynchronously
     */
    private class ServerThread extends Thread {
        @Override
        public void run() {
            while(!socket.isClosed()) {
                update();
            }
        }
    }

}
