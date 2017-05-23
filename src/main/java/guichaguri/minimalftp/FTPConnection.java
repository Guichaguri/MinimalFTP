package guichaguri.minimalftp;

import guichaguri.minimalftp.api.IFileSystem;
import guichaguri.minimalftp.api.ResponseException;
import guichaguri.minimalftp.handler.ConnectionHandler;
import guichaguri.minimalftp.handler.FileHandler;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Represents a FTP user connected to the server
 * @author Guilherme Chaguri
 */
public class FTPConnection implements Closeable {

    protected final FTPServer server;
    protected final Socket con;
    protected final BufferedReader reader;
    protected final BufferedWriter writer;
    protected final ConnectionThread thread;

    protected ConnectionHandler conHandler;
    protected FileHandler fileHandler;

    protected FTPConnection(FTPServer server, Socket con) throws IOException {
        this.server = server;
        this.con = con;
        this.reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));

        this.thread = new ConnectionThread();
        this.thread.start();

        this.conHandler = new ConnectionHandler(this);
        this.fileHandler = new FileHandler(this);

        this.conHandler.onConnected();
        this.fileHandler.onConnected();
    }

    /**
     * The server which the connection belongs to
     */
    public FTPServer getServer() {
        return server;
    }

    /**
     * Gets the connection address
     */
    public InetAddress getAddress() {
        return con.getInetAddress();
    }

    /**
     * Whether the connection is authenticated
     */
    public boolean isAuthenticated() {
        return conHandler.isAuthenticated();
    }

    /**
     * The username of the connection. May be {@code null}
     */
    public String getUsername() {
        return conHandler.getUsername();
    }

    /**
     * The file system of the connection. May be {@code null} when it's still authenticating
     */
    public IFileSystem getFileSystem() {
        return fileHandler.getFileSystem();
    }

    /**
     * Sets the new file system for this connection.
     * Calling this method can result into desynchronization for the connection.
     * Please, if you want to change the file system, use a {@link guichaguri.minimalftp.api.IUserAuthenticator}
     *
     * @param fs The new file system
     */
    public void setFileSystem(IFileSystem fs) {
        fileHandler.setFileSystem(fs);
    }

    /**
     * Sends a response to the connection
     * @param code The response code
     * @param response The response message
     */
    public void sendResponse(int code, String response) {
        if(con.isClosed()) return;

        try {
            writer.write(code + " " + response + "\r\n");
            writer.flush();
        } catch(IOException ex) {
            Utils.closeQuietly(this);
        }
    }

    /**
     * Sends an array of bytes through a data connection
     * @param data The data to be sent
     * @throws ResponseException When an error occurs
     */
    public void sendData(byte[] data) throws ResponseException {
        if(con.isClosed()) return;

        try {
            Socket socket = conHandler.createDataSocket();
            OutputStream out = socket.getOutputStream();

            Utils.write(out, data, data.length, conHandler.isAsciiMode());

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(socket);
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while opening the data connection");
        }
    }

    /**
     * Sends a stream through a data connection
     * @param in The input stream
     * @throws ResponseException When an error occurs
     */
    public void sendData(InputStream in) throws ResponseException {
        if(con.isClosed()) return;

        try {
            Socket socket = conHandler.createDataSocket();
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                Utils.write(out, buffer, len, conHandler.isAsciiMode());
            }

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(socket);
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while opening the data connection");
        }
    }

    /**
     * Receives a stream through the data connection
     * @param out The output stream
     * @throws ResponseException When an error occurs
     */
    public void receiveData(OutputStream out) throws ResponseException {
        if(con.isClosed()) return;

        try {
            Socket socket = conHandler.createDataSocket();
            InputStream in = socket.getInputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(socket);
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while opening the data connection");
        }
    }

    /**
     * Processes commands
     */
    protected boolean process(String[] cmd) {
        try {
            if(conHandler.onCommand(cmd)) return true;
            if(fileHandler.onCommand(cmd)) return true;
        } catch(ResponseException ex) {
            sendResponse(ex.getCode(), ex.getMessage());
            return true;
        } catch(FileNotFoundException ex) {
            sendResponse(550, ex.getMessage());
            return true;
        } catch(IOException ex) {
            sendResponse(450, ex.getMessage());
            return true;
        } catch(Exception ex) {
            sendResponse(451, ex.getMessage());
            ex.printStackTrace();
            return true;
        }

        return false;
    }

    /**
     * Updates the connection
     */
    protected void update() throws IOException {
        if(conHandler.shouldStop()) {
            Utils.closeQuietly(this);
            return;
        }

        String line = reader.readLine();

        if(line == null) {
            Utils.closeQuietly(this);
            return;
        }

        if(line.isEmpty()) return;

        String[] cmd = line.split("\\s+");
        cmd[0] = cmd[0].toUpperCase();

        if(!process(cmd)) {
            System.out.println("Unknown command: " + line);//TODO remove debug
            sendResponse(502, "Unknown command");
        } else {
            System.out.println("Command: " + line);
        }
    }

    @Override
    public void close() throws IOException {
        if(thread != null) {
            thread.interrupt();
        }

        conHandler.onDisconnected();
        fileHandler.onDisconnected();

        con.close();

        server.removeConnection(this);
    }

    /**
     * Thread that processes this connection when multithreading is enabled
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            while(!con.isClosed()) {
                try {
                    update();
                } catch(IOException ex) {
                    ex.printStackTrace();//TODO handle this
                }
            }
        }
    }
}
