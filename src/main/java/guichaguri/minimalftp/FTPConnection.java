package guichaguri.minimalftp;

import guichaguri.minimalftp.api.IFileSystem;
import guichaguri.minimalftp.api.ResponseException;
import guichaguri.minimalftp.handler.ConnectionHandler;
import guichaguri.minimalftp.handler.FileHandler;
import java.io.*;
import java.net.Socket;

/**
 * @author Guilherme Chaguri
 */
public class FTPConnection implements Closeable, Runnable {

    private final FTPServer server;
    private final Socket con;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    private ConnectionHandler conHandler;
    private FileHandler fileHandler;

    protected FTPConnection(FTPServer server, Socket con) throws IOException {
        this.server = server;
        this.con = con;
        this.reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));

        this.conHandler = new ConnectionHandler(this);
        this.fileHandler = new FileHandler(this);

        this.conHandler.onConnected();
        this.fileHandler.onConnected();
    }

    public FTPServer getServer() {
        return server;
    }

    public boolean isAuthenticated() {
        return conHandler.isAuthenticated();
    }

    public String getUsername() {
        return conHandler.getUsername();
    }

    public IFileSystem getFileSystem() {
        return fileHandler.getFileSystem();
    }

    public void setFileSystem(IFileSystem fs) {
        fileHandler.setFileSystem(fs);
    }

    public void sendResponse(int code, String response) {
        try {
            writer.write(code + " " + response + "\r\n");
            writer.flush();
        } catch(IOException ex) {
            //TODO handle
            ex.printStackTrace();
        }
    }

    public void sendData(byte[] data) throws ResponseException {
        try {
            Socket socket = conHandler.createDataSocket();
            OutputStream out = socket.getOutputStream();

            Utils.write(out, data, data.length, conHandler.isAsciiMode());

            out.flush();
            out.close();
            socket.close();
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while opening the data connection");
        }
    }

    public void sendData(InputStream in) throws ResponseException {
        try {
            Socket socket = conHandler.createDataSocket();
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                Utils.write(out, buffer, len, conHandler.isAsciiMode());
            }

            out.flush();
            out.close();
            in.close();
            socket.close();
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while opening the data connection");
        }
    }

    public void receiveData(OutputStream out) throws IOException {
        Socket socket = conHandler.createDataSocket();
        InputStream in = socket.getInputStream();

        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }

        out.flush();
        out.close();
        in.close();
        socket.close();
    }

    protected boolean process(String[] cmd) {
        try {
            if(conHandler.onCommand(cmd)) return true;
            if(fileHandler.onCommand(cmd)) return true;
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    public void run() {
        try {
            while(!conHandler.shouldStop() && !con.isClosed()) {
                String line = reader.readLine();
                if(line == null) break;
                if(line.isEmpty()) continue;

                String[] cmd = line.split("\\s+");
                cmd[0] = cmd[0].toUpperCase();

                if(!process(cmd)) {
                    System.out.println("Unknown command: " + line);//TODO remove debug
                    sendResponse(502, "Unknown command");
                } else {
                    System.out.println("Command: " + line);
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        conHandler.onDisconnected();
        fileHandler.onDisconnected();

        con.close();
    }
}
