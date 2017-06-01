package com.guichaguri.minimalftp;

import com.guichaguri.minimalftp.impl.NativeFileSystem;
import com.guichaguri.minimalftp.impl.NoOpAuthenticator;
import java.io.File;
import java.io.IOException;

/**
 * A simple API example
 * @author Guilherme Chaguri
 */
public class SimpleServer {

    public static void main(String[] args) throws IOException {
        // Uses the current working directory as the root
        File root = new File(System.getProperty("user.dir"));

        // Creates a native file system
        NativeFileSystem fs = new NativeFileSystem(root);

        // Creates a noop authenticator
        NoOpAuthenticator auth = new NoOpAuthenticator(fs);

        // Creates the server with the authenticator
        FTPServer server = new FTPServer(auth);

        // Start listening synchronously
        server.listenSync(21);
    }

}
