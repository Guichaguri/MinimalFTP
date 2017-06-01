package com.guichaguri.minimalftp;

import com.guichaguri.minimalftp.api.IFileSystem;
import com.guichaguri.minimalftp.api.IUserAuthenticator;
import com.guichaguri.minimalftp.handler.FileHandler;
import com.guichaguri.minimalftp.api.CommandInfo;
import com.guichaguri.minimalftp.api.CommandInfo.Command;
import com.guichaguri.minimalftp.api.CommandInfo.NoArgsCommand;
import com.guichaguri.minimalftp.api.CommandInfo.ArgsArrayCommand;
import com.guichaguri.minimalftp.api.ResponseException;
import com.guichaguri.minimalftp.handler.ConnectionHandler;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a FTP user connected to the server
 * @author Guilherme Chaguri
 */
public class FTPConnection implements Closeable {

    protected final Map<String, CommandInfo> commands = new HashMap<>();
    protected final Map<String, CommandInfo> siteCommands = new HashMap<>();

    protected final FTPServer server;
    protected final Socket con;
    protected final BufferedReader reader;
    protected final BufferedWriter writer;
    protected final ConnectionThread thread;
    protected final ArrayDeque<Socket> dataConnections = new ArrayDeque<>();

    protected ConnectionHandler conHandler;
    protected FileHandler fileHandler;

    protected long bytesTransferred = 0;
    protected boolean responseSent = true;
    protected int timeout = 0;
    protected long lastUpdate = 0;

    /**
     * Creates a new FTP connection.
     * Usually initialized by a {@link FTPServer}
     *
     * @param server The server which received the connection
     * @param con The connection socket
     * @param idleTimeout The timeout in milliseconds
     * @throws IOException When an I/O error occurs
     */
    public FTPConnection(FTPServer server, Socket con, int idleTimeout) throws IOException {
        this.server = server;
        this.con = con;
        this.reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));

        this.timeout = idleTimeout;
        this.lastUpdate = System.currentTimeMillis();
        con.setSoTimeout(timeout);

        this.conHandler = new ConnectionHandler(this);
        this.fileHandler = new FileHandler(this);

        this.thread = new ConnectionThread();
        this.thread.start();

        registerCommand("SITE", "SITE <command>", this::site);

        this.conHandler.registerCommands();
        this.fileHandler.registerCommands();
        this.conHandler.onConnected();
    }

    /**
     * The server which the connection belongs to
     * @return The {@link FTPServer} that received this connection
     */
    public FTPServer getServer() {
        return server;
    }

    /**
     * Gets the connection address
     * @return The {@link InetAddress} of this connection
     */
    public InetAddress getAddress() {
        return con.getInetAddress();
    }

    /**
     * Gets the amount of bytes sent or received
     * @return The number of bytes
     */
    public long getBytesTransferred() {
        return bytesTransferred;
    }

    /**
     * Whether the connection is authenticated
     * @return {@code true} when it's authenticated, {@code false} otherwise
     */
    public boolean isAuthenticated() {
        return conHandler.isAuthenticated();
    }

    /**
     * Gets the username of the connection.
     * @return The username or {@code null}
     */
    public String getUsername() {
        return conHandler.getUsername();
    }

    /**
     * Whether the connection is in ASCII instead of Binary
     * @return {@code true} for ASCII, {@code false} for Binary
     */
    public boolean isAsciiMode() {
        return conHandler.isAsciiMode();
    }

    /**
     * The file system of the connection. May be {@code null} when it's still authenticating
     * @return The current file system
     */
    public IFileSystem getFileSystem() {
        return fileHandler.getFileSystem();
    }

    /**
     * Sets the new file system for this connection.
     * Calling this method can result into desynchronization for the connection.
     * Please, if you want to change the file system, use a {@link IUserAuthenticator}
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
        responseSent = true;
    }

    /**
     * Sends an array of bytes through a data connection
     * @param data The data to be sent
     * @throws ResponseException When an error occurs
     */
    public void sendData(byte[] data) throws ResponseException {
        if(con.isClosed()) return;

        Socket socket = null;
        try {
            socket = conHandler.createDataSocket();
            dataConnections.add(socket);
            OutputStream out = socket.getOutputStream();

            Utils.write(out, data, data.length, conHandler.isAsciiMode());
            bytesTransferred += data.length;

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(socket);
        } catch(SocketException ex) {
            throw new ResponseException(426, "Transfer aborted");
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        } finally {
            onUpdate();
            if(socket != null) dataConnections.remove(socket);
        }
    }

    /**
     * Sends a stream through a data connection
     * @param in The input stream
     * @throws ResponseException When an error occurs
     */
    public void sendData(InputStream in) throws ResponseException {
        if(con.isClosed()) return;

        Socket socket = null;
        try {
            socket = conHandler.createDataSocket();
            dataConnections.add(socket);
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                Utils.write(out, buffer, len, conHandler.isAsciiMode());
                bytesTransferred += len;
            }

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(socket);
        } catch(SocketException ex) {
            throw new ResponseException(426, "Transfer aborted");
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        } finally {
            onUpdate();
            if(socket != null) dataConnections.remove(socket);
        }
    }

    /**
     * Receives a stream through the data connection
     * @param out The output stream
     * @throws ResponseException When an error occurs
     */
    public void receiveData(OutputStream out) throws ResponseException {
        if(con.isClosed()) return;

        Socket socket = null;
        try {
            socket = conHandler.createDataSocket();
            dataConnections.add(socket);
            InputStream in = socket.getInputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                bytesTransferred += len;
            }

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(socket);
        } catch(SocketException ex) {
            throw new ResponseException(426, "Transfer aborted");
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        } finally {
            onUpdate();
            if(socket != null) dataConnections.remove(socket);
        }
    }

    /**
     * Aborts all data transfers
     */
    public void abortDataTransfers() {
        while(!dataConnections.isEmpty()) {
            Socket socket = dataConnections.poll();
            if(socket != null) Utils.closeQuietly(socket);
        }
    }

    public void registerSiteCommand(String label, String help, Command cmd) {
        addSiteCommand(label, help, cmd);
    }

    public void registerSiteCommand(String label, String help, NoArgsCommand cmd) {
        addSiteCommand(label, help, cmd);
    }

    public void registerSiteCommand(String label, String help, ArgsArrayCommand cmd) {
        addSiteCommand(label, help, cmd);
    }

    public void registerCommand(String label, String help, Command cmd) {
        addCommand(label, help, cmd, true);
    }

    public void registerCommand(String label, String help, NoArgsCommand cmd) {
        addCommand(label, help, cmd, true);
    }

    public void registerCommand(String label, String help, ArgsArrayCommand cmd) {
        addCommand(label, help, cmd, true);
    }

    public void registerCommand(String label, String help, Command cmd, boolean needsAuth) {
        addCommand(label, help, cmd, needsAuth);
    }

    public void registerCommand(String label, String help, NoArgsCommand cmd, boolean needsAuth) {
        addCommand(label, help, cmd, needsAuth);
    }

    public void registerCommand(String label, String help, ArgsArrayCommand cmd, boolean needsAuth) {
        addCommand(label, help, cmd, needsAuth);
    }

    /**
     * Internally registers a SITE command
     * @param label The command name
     * @param help The help message
     * @param cmd The command function
     */
    protected void addSiteCommand(String label, String help, Command cmd) {
        siteCommands.put(label.toUpperCase(), new CommandInfo(cmd, help, true));
    }

    /**
     * Internally registers a command
     * @param label The command name
     * @param help The help message
     * @param cmd The command function
     * @param needsAuth Whether authentication is required to run this command
     */
    protected void addCommand(String label, String help, Command cmd, boolean needsAuth) {
        commands.put(label.toUpperCase(), new CommandInfo(cmd, help, needsAuth));
    }

    /**
     * Gets the help message from a SITE command
     * @param label The command name
     * @return The help message or {@code null} if the command was not found
     */
    public String getSiteHelpMessage(String label) {
        CommandInfo info = siteCommands.get(label);
        return info != null ? info.help : null;
    }

    /**
     * Gets the help message from a command
     * @param label The command name
     * @return The help message or {@code null} if the command was not found
     */
    public String getHelpMessage(String label) {
        CommandInfo info = commands.get(label);
        return info != null ? info.help : null;
    }

    protected void onUpdate() {
        lastUpdate = System.currentTimeMillis();
    }

    /**
     * Processes commands
     * @param cmd The command and its arguments
     */
    protected void process(String cmd) {
        int firstSpace = cmd.indexOf(' ');
        if(firstSpace < 0) firstSpace = cmd.length();

        CommandInfo info = commands.get(cmd.substring(0, firstSpace).toUpperCase());

        if(info == null) {
            sendResponse(502, "Unknown command");
            return;
        }

        processCommand(info, firstSpace != cmd.length() ? cmd.substring(firstSpace + 1) : "");
    }

    /**
     * SITE command
     * @param cmd The command and its arguments
     */
    protected void site(String cmd) {
        int firstSpace = cmd.indexOf(' ');
        if(firstSpace < 0) firstSpace = cmd.length();

        CommandInfo info = siteCommands.get(cmd.substring(0, firstSpace).toUpperCase());

        if(info == null) {
            sendResponse(504, "Unknown site command");
            return;
        }

        processCommand(info, firstSpace != cmd.length() ? cmd.substring(firstSpace + 1) : "");
    }

    protected void processCommand(CommandInfo info, String args) {
        if(info.needsAuth && !conHandler.isAuthenticated()) {
            sendResponse(530, "Needs authentication");
            return;
        }

        responseSent = false;

        try {
            info.command.run(info, args);
        } catch(ResponseException ex) {
            sendResponse(ex.getCode(), ex.getMessage());
        } catch(FileNotFoundException ex) {
            sendResponse(550, ex.getMessage());
        } catch(IOException ex) {
            sendResponse(450, ex.getMessage());
        } catch(Exception ex) {
            sendResponse(451, ex.getMessage());
            ex.printStackTrace();
        }

        if(!responseSent) sendResponse(200, "Done");
    }

    /**
     * Updates the connection
     */
    protected void update() {
        if(conHandler.shouldStop()) {
            Utils.closeQuietly(this);
            return;
        }

        String line;

        try {
            line = reader.readLine();
        } catch(SocketTimeoutException ex) {
            // Check if the socket has timed out
            if(!dataConnections.isEmpty() && (System.currentTimeMillis() - lastUpdate) >= timeout) {
                Utils.closeQuietly(this);
            }
            return;
        } catch(IOException ex) {
            return;
        }

        if(line == null) {
            Utils.closeQuietly(this);
            return;
        }

        if(line.isEmpty()) return;

        process(line);
    }

    /**
     * Stops the connection, but does not removes it from the list.
     * For a complete cleanup, use {@link #close()} instead
     * @throws IOException When an I/O error occurs
     */
    protected void stop() throws IOException {
        if(!thread.isInterrupted()) {
            thread.interrupt();
        }

        conHandler.onDisconnected();

        con.close();
    }

    @Override
    public void close() throws IOException {
        stop();

        server.removeConnection(this);
    }

    /**
     * Thread that processes this connection
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            while(!con.isClosed()) {
                update();
            }
        }
    }
}
