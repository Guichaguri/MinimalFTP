package com.guichaguri.minimalftp.custom;

import com.guichaguri.minimalftp.CustomServer;
import com.guichaguri.minimalftp.FTPConnection;
import java.io.IOException;

/**
 * A class that handle a custom command.
 * You can easily add more commands with more methods
 *
 * The commands are registered in {@link CustomServer#onConnected(FTPConnection)}
 * @author Guilherme Chaguri
 */
public class CommandHandler {

    private final FTPConnection con;

    public CommandHandler(FTPConnection con) {
        this.con = con;
    }

    // Our custom command
    public void customCommand(String argument) throws IOException {
        // In FileZilla, you can run custom commands at "Server" -> "Enter custom command"
        System.out.println("Here is the argument: " + argument);

        // You can send custom responses for this command
        // If you don't store the FTPConnection, you can use throw an exception instead of using con.sendResponse
        // Check for existing response codes in the specification ( https://tools.ietf.org/html/rfc959#page-39 )
        // If you don't send any response, a "200 Done" will be automatically sent
        con.sendResponse(200, ":D");
        /*throw new ResponseException(200, ":D");*/
    }

}
