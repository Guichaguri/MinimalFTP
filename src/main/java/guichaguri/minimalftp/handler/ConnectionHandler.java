package guichaguri.minimalftp.handler;

import guichaguri.minimalftp.FTPConnection;
import guichaguri.minimalftp.api.ICommandHandler;
import guichaguri.minimalftp.api.IUserAuthenticator;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Guilherme Chaguri
 */
public class ConnectionHandler implements ICommandHandler {

    private final FTPConnection con;

    private boolean authenticated = false;
    private String username = null;

    private boolean passive = false;
    private ServerSocket passiveServer = null;
    private String activeHost = null;
    private int activePort = 0;

    private boolean ascii = true;

    private boolean stop = false;

    public ConnectionHandler(FTPConnection connection) {
        this.con = connection;
    }

    public boolean shouldStop() {
        return stop;
    }

    public boolean isAsciiMode() {
        return ascii;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getUsername() {
        return username;
    }

    public Socket createDataSocket() throws IOException {
        if(passive && passiveServer != null) {
            return passiveServer.accept();
        } else {
            return new Socket(activeHost, activePort);
        }
    }

    @Override
    public void onConnected() throws IOException {
        IUserAuthenticator auth = con.getServer().getAuthenticator();

        if(!auth.needsUsername(con)) {
            if(authenticate(auth, null)) {
                con.sendResponse(220, "Ready!");
            } else {
                con.sendResponse(421, "Authentication failed");
                con.close();
            }
        } else {
            con.sendResponse(530, "Waiting for authentication...");
        }
    }

    @Override
    public boolean onCommand(String[] cmd) throws IOException {
        if(cmd[0].equals("NOOP")) { // Ping (NOOP)
            con.sendResponse(200, "OK");
        } else if(cmd[0].equals("QUIT")) { // Quit (QUIT)
            quit();
        } else if(cmd[0].equals("REIN")) { // Logout (REIN)
            rein();
        } else if(cmd[0].equals("USER")) { // Set Username (USER <username>)
            user(cmd[1]);
        } else if(cmd[0].equals("PASS")) { // Set Password (PASS <password>)
            pass(cmd[1]);
        } else if(!authenticated) {
            con.sendResponse(530, "Needs authentication");
        } else if(cmd[0].equals("ACCT")) { // Account Info (ACCT <info>)
            con.sendResponse(230, "Logged in!");
        } else if(cmd[0].equals("SYST")) { // System Information (SYST)
            con.sendResponse(215, "UNIX Type: L8"); // Generic System Info
        } else if(cmd[0].equals("PORT")) { // Active Mode (PORT <host-port>)
            port(cmd[1]);
        } else if(cmd[0].equals("PASV")) { // Passive Mode (PASV)
            pasv();
        } else if(cmd[0].equals("TYPE")) { // Binary Flag (TYPE <type>)
            type(cmd[1]);
        } else if(cmd[0].equals("STRU")) { // Structure Type (STRU <type>)
            stru(cmd[1]);
        } else if(cmd[0].equals("MODE")) { // Change Mode (MODE <mode>)
            mode(cmd[1]);
        } else if(cmd[0].equals("STAT")) { // Statistics (STAT)
            stat();
        } else {
            return false;
        }

        return true;
    }

    @Override
    public void onDisconnected() throws IOException {
        if(passiveServer != null) {
            passiveServer.close();
        }
    }

    private void type(String type) throws IOException {
        if(type.startsWith("A")) {
            ascii = true;
        } else if(type.startsWith("L") || type.startsWith("I")) {
            ascii = false;
        } else {
            con.sendResponse(500, "Unknown type " + type);
            return;
        }
        con.sendResponse(200, "Type set to " + type);
    }

    private void stru(String structure) throws IOException {
        if(structure.equals("F")) {
            con.sendResponse(200, "The structure type was set to file");
        } else {
            con.sendResponse(504, "Unsupported structure type");
        }
    }

    private void mode(String mode) throws IOException {
        if(mode.equalsIgnoreCase("S")) {
            con.sendResponse(200, "The mode was set to stream");
        } else {
            con.sendResponse(504, "Unsupported mode");
        }
    }

    private void user(String username) throws IOException {
        if(authenticated) {
            con.sendResponse(230, "Logged in!");
            return;
        }

        this.username = username;

        IUserAuthenticator auth = con.getServer().getAuthenticator();
        if(auth.needsPassword(con, username)) {
            // Requests a password for the authentication
            con.sendResponse(331, "Needs password");
        } else {
            // Tries to authenticate using the given username
            boolean success = authenticate(auth, null);

            if(success) {
                con.sendResponse(230, "Logged in!");
            } else {
                con.sendResponse(530, "Authentication failed");
                con.close();
            }
        }
    }

    private void pass(String password) throws IOException {
        if(authenticated) {
            con.sendResponse(230, "Logged in!");
            return;
        }

        // Tries to authenticate using the given username and password
        boolean success = authenticate(con.getServer().getAuthenticator(), password);

        if(success) {
            con.sendResponse(230, "Logged in!");
        } else {
            con.sendResponse(530, "Authentication failed");
            con.close();
        }
    }

    private void rein() {
        authenticated = false;
        username = null;
        con.sendResponse(220, "Ready for a new user");
    }

    private void quit() {
        con.sendResponse(221, "Closing connection...");
        stop = true;
    }

    private boolean authenticate(IUserAuthenticator auth, String password) {
        try {
            con.setFileSystem(auth.authenticate(con, username, password));
            authenticated = true;
            return true;
        } catch(Exception ex) {
            return false;
        }
    }

    private void pasv() throws IOException {
        passiveServer = new ServerSocket(0, 5, con.getServer().getAddress());
        passive = true;

        String host = passiveServer.getInetAddress().getHostAddress();
        int port = passiveServer.getLocalPort();

        if(host.equals("0.0.0.0")) host = "127.0.0.1";//TODO remove

        String[] addr = host.split("\\.");

        String address = addr[0] + "," + addr[1] + "," + addr[2] + "," + addr[3];
        String addressPort = port / 256 + "," + port % 256;

        con.sendResponse(227, "Enabling Passive Mode (" + address + "," + addressPort + ")");
    }

    private void port(String data) {
        String[] args = data.split(",");

        activeHost = args[0] + "." + args[1] + "." + args[2] + "." + args[3];
        activePort = Integer.parseInt(args[4]) * 256 + Integer.parseInt(args[5]);
        passive = false;
    }

    private void stat() throws IOException {
        con.sendResponse(211, "Sending the status...");
        String data = "";
        String ip = con.getAddress().getHostAddress();
        data += "Connected from " + ip + " (" + ip + ")";
        data += "Logged in " + (username != null ? "as " + username : "anonymously");
        data += "TYPE: " + (ascii ? "ASCII" : "Binary") + ", STRUcture: File, Mode: Stream";
        data += "Total bytes transferred for session: " + con.getBytesTransferred();
        con.sendData(data.getBytes("UTF-8"));
        con.sendResponse(211, "Status sent!");
    }

}
