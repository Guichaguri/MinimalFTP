package guichaguri.minimalftp.api;

import guichaguri.minimalftp.FTPConnection;

/**
 * @author Guilherme Chaguri
 */
public interface IUserAuthenticator {

    /**
     * Whether this authenticator requires a username
     */
    boolean needsUsername(FTPConnection con);

    /**
     * Whether this authenticator requires a password
     * Only affects when {@link #needsUsername(FTPConnection)} is also {@code true}
     */
    boolean needsPassword(FTPConnection con, String username);

    /**
     * Authenticates a user
     * You use custom file systems depending on the user
     *
     * @param username The username or {@code null} when {@link #needsUsername(FTPConnection)} returns false
     * @param password The password or {@code null} when {@link #needsPassword(FTPConnection, String)} returns false
     * @return A file system if the authentication succeeded
     * @throws AuthException When the authentication failed
     */
    IFileSystem authenticate(FTPConnection con, String username, String password) throws AuthException;


    /**
     * The exception that should be thrown when the authentication fails
     */
    class AuthException extends RuntimeException { }

}
