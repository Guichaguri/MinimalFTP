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

    private final List<FTPConnection> connections = Collections.synchronizedList(new ArrayList<FTPConnection>());

    private IUserAuthenticator auth = null;

    private ServerSocket socket = null;
    private boolean multithreaded;
    private ServerThread serverThread = null;
    private ConnectionThread conThread = null;

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
        listen(null, port, false);
    }

    /**
     * Starts the FTP server asynchronously
     *
     * @param address The server address or {@code null} for a local address
     * @param port The server port or {@code 0} to automatically allocate the port
     * @param multithreaded When {@code true}, each connection will run in their own thread.
     *                      When {@code false}, all connections will run in a single thread.
     * @throws IOException
     */
    public void listen(InetAddress address, int port, boolean multithreaded) throws IOException {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        if(socket != null) throw new IOException("Server already started");

        socket = new ServerSocket(port, 50, address);
        this.multithreaded = multithreaded;

        if(!multithreaded) {
            conThread = new ConnectionThread();
            conThread.start();
        }

        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * Starts the FTP server synchronously
     * It will block the current thread
     *
     * @param port The server port
     * @throws IOException
     */
    public void listenSync(int port) throws IOException {
        listenSync(null, port, false);
    }

    /**
     * Starts the FTP server synchronously
     * It will block the current thread
     *
     * @param address The server address or {@code null} for a local address
     * @param port The server port or {@code 0} to automatically allocate the port
     * @param multithreaded When {@code true}, each connection will run in their own thread.
     *                      When {@code false}, all connections will run in a single thread.
     * @throws IOException
     */
    public void listenSync(InetAddress address, int port, boolean multithreaded) throws IOException {
        if(auth == null) throw new NullPointerException("The Authenticator is null");
        if(socket != null) throw new IOException("Server already started");

        socket = new ServerSocket(port, 50, address);
        this.multithreaded = multithreaded;

        if(!multithreaded) {
            conThread = new ConnectionThread();
            conThread.start();
        }

        while(!socket.isClosed()) {
            update();
        }
    }

    protected void update() {
        try {
            Socket connection = socket.accept();
            System.out.println("New connection: " + connection.getInetAddress().toString());//TODO remove debug
            synchronized(connections) {
                connections.add(new FTPConnection(this, connection, multithreaded));
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void updateConnections() {
        synchronized(connections) {
            for(FTPConnection con : connections) {
                try {
                    con.update(false);
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    protected void removeConnection(FTPConnection con) {
        System.out.println("Removing connection " + con.getAddress().toString());
        synchronized(connections) {
            connections.remove(con);
        }
    }

    protected void stop() {
        if(serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        if(conThread != null) {
            conThread.interrupt();
            conThread = null;
        }

        for(FTPConnection con : connections) {
            try {
                con.close();
            } catch(IOException ex) {}
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

    /**
     * Thread that processes all connections when multithreading is disabled
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            while(!socket.isClosed()) {
                updateConnections();
                try {
                    Thread.sleep(100);
                } catch(InterruptedException ex) {}
            }
        }
    }

}
