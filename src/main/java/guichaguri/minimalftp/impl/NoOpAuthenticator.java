package guichaguri.minimalftp.impl;

import guichaguri.minimalftp.FTPConnection;
import guichaguri.minimalftp.api.IFileSystem;
import guichaguri.minimalftp.api.IUserAuthenticator;

/**
 * No Operation Authenticator
 * Allows any user in with a predefined file system
 * @author Guilherme Chaguri
 */
public class NoOpAuthenticator implements IUserAuthenticator {

    private final IFileSystem fs;

    /**
     * Creates the authenticator
     * @param fs A file system
     * @see NativeFileSystem
     */
    public NoOpAuthenticator(IFileSystem fs) {
        this.fs = fs;
    }

    @Override
    public boolean needsUsername(FTPConnection con) {
        return false;
    }

    @Override
    public boolean needsPassword(FTPConnection con, String username) {
        return false;
    }

    @Override
    public IFileSystem authenticate(FTPConnection con, String username, String password) throws AuthException {
        return fs;
    }
}
