package com.guichaguri.minimalftp.handler;

import com.guichaguri.minimalftp.FTPConnection;
import com.guichaguri.minimalftp.Utils;
import com.guichaguri.minimalftp.api.IUserAuthenticator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLContext;

/**
 * Handles special connection-based commands
 * @author Guilherme Chaguri
 */
public class ConnectionHandler {

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

    public void onConnected() throws IOException {
        IUserAuthenticator auth = con.getServer().getAuthenticator();

        if(!auth.needsUsername(con)) {
            if(authenticate(auth, null)) {
                con.sendResponse(230, "Ready!");
            } else {
                con.sendResponse(421, "Authentication failed");
                con.close();
            }
        } else {
            con.sendResponse(220, "Waiting for authentication...");
        }
    }

    public void onDisconnected() throws IOException {
        if(passiveServer != null) {
            Utils.closeQuietly(passiveServer);
            passiveServer = null;
        }
    }

    public void registerCommands() {
        con.registerCommand("NOOP", "NOOP", this::noop, false); // Ping
        con.registerCommand("HELP", "HELP <command>", this::help, false); // Command Help
        con.registerCommand("QUIT", "QUIT", this::quit, false); // Quit
        con.registerCommand("REIN", "REIN", this::rein, false); // Logout
        con.registerCommand("USER", "USER <username>", this::user, false); // Set Username
        con.registerCommand("PASS", "PASS <password>", this::pass, false); // Set Password
        con.registerCommand("ACCT", "ACCT <info>", this::acct, false); // Account Info
        con.registerCommand("SYST", "SYST", this::syst); // System Information
        con.registerCommand("PORT", "PORT <host-port>", this::port); // Active Mode
        con.registerCommand("PASV", "PASV", this::pasv); // Passive Mode
        con.registerCommand("TYPE", "TYPE <type>", this::type); // Binary Flag
        con.registerCommand("STRU", "STRU <type>", this::stru); // Structure Type
        con.registerCommand("MODE", "MODE <mode>", this::mode); // Change Mode
        con.registerCommand("STAT", "STAT", this::stat); // Statistics

        con.registerCommand("AUTH", "AUTH <mechanism>", this::auth, false); // Security Mechanism (RFC 2228)
    }

    private void noop() {
        con.sendResponse(200, "OK");
    }

    private void help(String[] cmd) {
        if(cmd.length < 1) {
            con.sendResponse(501, "Missing parameters");
        }

        String command = cmd[0].toUpperCase();
        String help;

        if(cmd.length > 1 && command.equals("SITE")) {
            help = "SITE " + con.getSiteHelpMessage(cmd[1].toUpperCase());
        } else {
            help = con.getHelpMessage(command);
        }
        con.sendResponse(214, help);
    }

    private void type(String type) throws IOException {
        type = type.toUpperCase();

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
        if(structure.equalsIgnoreCase("F")) {
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
            con.sendResponse(331, "Needs a password");
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

    private void acct(String info) {
        if(authenticated) {
            con.sendResponse(230, "Logged in!");
            return;
        }

        // Many clients don't even support this command, it's not needed in most cases
        // A simple "username and password" combination is the most common system in the internet anyway
        // The authenticator can also handle special formatted usernames, if really needed (for instance: "username|account")

        // Although this is pretty simple to implement, I would have to store the password
        // in a field instead of directly sending it to the authenticator. I prefer to keep
        // things the way they are for security reasons.

        con.sendResponse(530, "Account information is not supported");
    }

    private void syst() {
        con.sendResponse(215, "UNIX Type: L8"); // Generic System Info
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

    private void pasv() throws IOException {
        passiveServer = new ServerSocket(0, 5, con.getServer().getAddress());
        passive = true;

        String host = passiveServer.getInetAddress().getHostAddress();
        int port = passiveServer.getLocalPort();

        if(host.equals("0.0.0.0")) {
            // Sends a valid address instead of a wildcard
            host = InetAddress.getLocalHost().getHostAddress();
        }

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

        if(passiveServer != null) {
            Utils.closeQuietly(passiveServer);
            passiveServer = null;
        }
    }

    private void stat() throws IOException {
        con.sendResponse(211, "Sending the status...");

        String ip = con.getAddress().getHostAddress();
        String user = username != null ? "as " + username : "anonymously";
        String type = ascii ? "ASCII" : "Binary";

        String data = "";
        data += "Connected from " + ip + " (" + ip + ")\r\n";
        data += "Logged in " + user + "\r\n";
        data += "TYPE: " + type + ", STRUcture: File, MODE: Stream\r\n";
        data += "Total bytes transferred for session: " + con.getBytesTransferred() + "\r\n";
        con.sendData(data.getBytes("UTF-8"));

        con.sendResponse(211, "Status sent!");
    }

    private void auth(String mechanism) throws IOException {
        mechanism = mechanism.toUpperCase();

        if(mechanism.equals("TLS") || mechanism.equals("TLS-C") ||
            mechanism.equals("SSL") || mechanism.equals("TLS-P")) {
            // No need to distinguish between TLS and SSL, as the protocol self-negotiate its level

            SSLContext ssl = con.getServer().getSSLContext();

            if(ssl == null) {
                con.sendResponse(431, "TLS/SSL is not available");
            } else if(con.isSSLEnabled()) {
                con.sendResponse(503, "TLS/SSL is already enabled");
            } else {
                con.sendResponse(234, "Enabling TLS/SSL...");
                con.enableSSL(ssl);
            }

        } else {
            con.sendResponse(502, "Unsupported mechanism");
        }
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
}
