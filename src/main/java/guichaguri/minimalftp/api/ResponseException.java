package guichaguri.minimalftp.api;

import java.io.IOException;

/**
 * @author Guilherme Chaguri
 */
public class ResponseException extends IOException {

    private final int code;

    public ResponseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
