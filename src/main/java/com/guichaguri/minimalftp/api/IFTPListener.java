package com.guichaguri.minimalftp.api;

import com.guichaguri.minimalftp.FTPConnection;

/**
 * Listens for events
 * @author Guilherme Chaguri
 */
public interface IFTPListener {

    /**
     * Triggered when a new connection is created
     * @param con The new connection
     */
    void onConnected(FTPConnection con);

    /**
     * Triggered when a connection disconnects
     * @param con The connection
     */
    void onDisconnected(FTPConnection con);

}
