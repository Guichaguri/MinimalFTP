package com.guichaguri.minimalftp;

import com.guichaguri.minimalftp.api.IFTPListener;
import com.guichaguri.minimalftp.api.IUserAuthenticator;
import com.guichaguri.minimalftp.impl.NoOpAuthenticator;
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
    protected final List<IFTPListener> listeners = Collections.synchronizedList(new ArrayList<IFTPListener>());

    protected IUserAuthenticator auth = null;
    protected int idleTimeout = 5 * 60 * 1000; // 5 minutes

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
     * @see NoOpAuthenticator
     */
    public void setAuthenticator(IUserAuthenticator auth) {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        this.auth = auth;
    }

    /**
     * Sets the idle timeout in milliseconds
     * Connections that are idle (no commands or transfers) for the specified time will be disconnected
     * The default and recommended time is 5 minutes
     *
     * @param idleTimeout The time in milliseconds
     */
    public void setTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Adds an {@link IFTPListener} to the server
     * @param listener The listener instance
     */
    public void addListener(IFTPListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an {@link IFTPListener} to the server
     * @param listener The listener instance
     */
    public void removeListener(IFTPListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Starts the FTP server asynchronously
     *
     * @param port The server port
     * @throws IOException When an error occurs while starting the server
     */
    public void listen(int port) throws IOException {
        listen(null, port);
    }

    /**
     * Starts the FTP server asynchronously
     *
     * @param address The server address or {@code null} for a local address
     * @param port The server port or {@code 0} to automatically allocate the port
     * @throws IOException When an error occurs while starting the server
     */
    public void listen(InetAddress address, int port) throws IOException {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        if(socket != null) throw new IOException("Server already started");

        socket = new ServerSocket(port, 50, address);

        serverThread = new ServerThread();
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Starts the FTP server synchronously
     * It will block the current thread
     * Connections to the server will still create new threads
     *
     * @param port The server port
     * @throws IOException When an error occurs while starting the server
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
     * @throws IOException When an error occurs while starting the server
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
            addConnection(socket.accept());
        } catch(IOException ex) {
            // The server was probably closed
        }
    }

    /**
     * Called when a connection is created
     * @param socket The connection socket
     * @throws IOException When an error occurs
     */
    protected void addConnection(Socket socket) throws IOException {
        FTPConnection con = new FTPConnection(this, socket, idleTimeout);

        synchronized(listeners) {
            for(IFTPListener l : listeners) {
                l.onConnected(con);
            }
        }
        synchronized(connections) {
            connections.add(con);
        }
    }

    /**
     * Called when a connection is terminated
     * @param con The FTP connection
     * @throws IOException When an error occurs
     */
    protected void removeConnection(FTPConnection con) throws IOException {
        synchronized(listeners) {
            for(IFTPListener l : listeners) {
                l.onDisconnected(con);
            }
        }
        synchronized(connections) {
            connections.remove(con);
        }
    }

    /**
     * Starts disposing server resources
     * For a complete cleanup, use {@link #close()} instead
     */
    protected void dispose() {
        // Terminates the server thread
        if(serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }

        // Stops each connection and clears them
        synchronized(connections) {
            for(FTPConnection con : connections) {
                try {
                    con.stop();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            connections.clear();
        }
    }

    @Override
    public void close() throws IOException {
        dispose();

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
            while(socket != null && !socket.isClosed()) {
                update();
            }
        }
    }

}
