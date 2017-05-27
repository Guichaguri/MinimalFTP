package guichaguri.minimalftp.custom;

import guichaguri.minimalftp.FTPConnection;
import guichaguri.minimalftp.api.IFileSystem;
import guichaguri.minimalftp.api.IUserAuthenticator;
import guichaguri.minimalftp.api.ResponseException;
import guichaguri.minimalftp.impl.NativeFileSystem;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Guilherme Chaguri
 */
public class UserbaseAuthenticator implements IUserAuthenticator {

    private final Map<String, byte[]> userbase = new HashMap<>();

    private byte[] toMD5(String pass) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(pass.getBytes("UTF-8"));
        } catch(Exception ex) {
            return pass.getBytes();
        }
    }

    public void registerUser(String username, String password) {
        userbase.put(username, toMD5(password));
    }

    @Override
    public boolean needsUsername(FTPConnection con) {
        return true;
    }

    @Override
    public boolean needsPassword(FTPConnection con, String username) {
        return true;
    }

    @Override
    public IFileSystem authenticate(FTPConnection con, String username, String password) throws AuthException {
        // Check for a user with that username in the database
        if(!userbase.containsKey(username)) {
            throw new AuthException();
        }

        // Gets the correct, original password
        byte[] originalPass = userbase.get(username);

        // Calculates the MD5 for the given password
        byte[] inputPass = toMD5(password);

        // Check for wrong password
        if(!Arrays.equals(originalPass, inputPass)) {
            throw new AuthException();
        }

        // We can even register custom commands for this user
        con.registerCommand("CUSTOM", "CUSTOM <string>", this::customCommand);

        // Use the username as a directory for file storage
        File path = new File(System.getProperty("user.dir"), "~" + username);
        return new NativeFileSystem(path);
    }

    private void customCommand(String argument) throws IOException {
        // In FileZilla, you can run custom commands at "Server" -> "Enter custom command"
        System.out.println("Here's the given argument: " + argument);

        // If you store the FTPConnection, you can use con.sendResponse instead of throwing an exception
        throw new ResponseException(200, ":D");
    }
}
