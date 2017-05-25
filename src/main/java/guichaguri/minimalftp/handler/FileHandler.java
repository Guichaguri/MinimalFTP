package guichaguri.minimalftp.handler;

import guichaguri.minimalftp.FTPConnection;
import guichaguri.minimalftp.Utils;
import guichaguri.minimalftp.api.ICommandHandler;
import guichaguri.minimalftp.api.IFileSystem;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Guilherme Chaguri
 */
@SuppressWarnings("unchecked")
public class FileHandler implements ICommandHandler {

    private final FTPConnection con;

    private IFileSystem fs = null;
    private Object cwd = null;

    private Object rnFile = null;

    public FileHandler(FTPConnection connection) {
        this.con = connection;
    }

    public IFileSystem getFileSystem() {
        return fs;
    }

    public void setFileSystem(IFileSystem fs) {
        this.fs = fs;
        this.cwd = fs.getRoot();
    }

    @Override
    public void onConnected() throws IOException {

    }

    @Override
    public boolean onCommand(String[] cmd) throws IOException {
        if(cmd[0].equals("CWD")) { // Change Working Dir (CWD <file>)
            cwd(cmd[1]);
        } else if(cmd[0].equals("CDUP")) { // Change to Parent Dir (CDUP)
            cdup();
        } else if(cmd[0].equals("PWD")) { // Retrieve Working Dir (PWD)
            pwd();
        } else if(cmd[0].equals("MKD")) { // Create Directory (MKD <file>)
            mkd(cmd[1]);
        } else if(cmd[0].equals("RMD")) { // Delete Directory (DELE <file>)
            rmd(cmd[1]);
        } else if(cmd[0].equals("DELE")) { // Delete File (DELE <file>)
            dele(cmd[1]);
        } else if(cmd[0].equals("LIST")) { // List Files (LIST [file])
            list(cmd.length > 1 ? cmd[1] : null);
        } else if(cmd[0].equals("NLST")) { // List File Names (NLST [file])
            nlst(cmd.length > 1 ? cmd[1] : null);
        } else if(cmd[0].equals("RETR")) { // Retrieve File (RETR <file>)
            retr(cmd[1]);
        } else if(cmd[0].equals("STOR")) { // Store File (STOR <file>)
            stor(cmd[1]);
        } else if(cmd[0].equals("STOU")) { // Store Random File (STOU [file])
            stou(cmd.length > 1 ? cmd[1] : null);
        } else if(cmd[0].equals("APPE")) { // Append File (APPE <file>)
            appe(cmd[1]);
        } else if(cmd[0].equals("ALLO")) { // Allocate Space (ALLO <size>)
            allo();
        } else if(cmd[0].equals("RNFR")) { // Rename From (RNFR <file>)
            rnfr(cmd[1]);
        } else if(cmd[0].equals("RNTO")) { // Rename To (RNTO <file>)
            rnto(cmd[1]);
        } else if(cmd[0].equals("SITE")) { // Special Commands (SITE <cmd>)
            if(cmd[1].equals("CHMOD")) { // Change Permissions (SITE CHMOD <perm> <file>)
                site_chmod(cmd[2], cmd[3]);
            }
        } else if(cmd[0].equals("MDTM")) { // Modification Time (MDTM <file>)
            mdtm(cmd[1]);
        } else if(cmd[0].equals("SIZE")) { // File Size (SIZE <file>)
            size(cmd[1]);
        } else {
            return false;
        }

        return true;
    }

    @Override
    public void onDisconnected() throws IOException {

    }

    private Object getFile(String path) throws IOException {
        if(path.equals("...") || path.equals("..")) {
            return fs.getParent(cwd);
        } else if(path.equals("/")) {
            return fs.getRoot();
        } else if(path.startsWith("/")) {
            return fs.findFile(fs.getRoot(), path.substring(1));
        } else {
            return fs.findFile(cwd, path);
        }
    }

    private void site_chmod(String perm, String file) throws IOException {
        fs.chmod(getFile(file), Utils.fromOctal(perm));
        con.sendResponse(200, "The file permissions were successfully changed");
    }

    private void cwd(String path) throws IOException {
        cwd = getFile(path);
        con.sendResponse(250, "The working directory was changed");
    }

    private void cdup() throws IOException {
        cwd = fs.getParent(cwd);
        con.sendResponse(200, "The working directory was changed");
    }

    private void pwd() {
        String path = "/" + fs.getPath(cwd);
        con.sendResponse(257, '"' + path + '"' + " CWD Name");
    }

    private void allo() {
        con.sendResponse(200, "There's no need to allocate space");
    }

    private void rnfr(String path) throws IOException {
        rnFile = getFile(path);
        con.sendResponse(350, "Rename request received");
    }

    private void rnto(String path) throws IOException {
        fs.rename(rnFile, getFile(path));
        rnFile = null;
        con.sendResponse(250, "File successfully renamed");
    }

    private void stor(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(150, "Receiving a file stream for " + path);
        con.receiveData(fs.writeFile(file, false));
        con.sendResponse(226, "File received!");
    }

    private void stou(String path) throws IOException {
        Object file = null;
        String ext = ".tmp";

        if(path != null) {
            file = getFile(path);
            int i = path.lastIndexOf('.');
            if(i > 0) ext = path.substring(i);
        }

        while(file != null && fs.exists(file)) {
            // Quick way to generate simple random names
            // It's not the "perfect" solution, as it only uses hexadecimal characters
            // But definitely enough for file names
            String name = UUID.randomUUID().toString().replace("-", "");
            file = fs.findFile(cwd, name + ext);
        }

        con.sendResponse(150, "Receiving a file stream for " + fs.getPath(file));
        con.receiveData(fs.writeFile(file, false));
        con.sendResponse(226, "File received!");
    }

    private void appe(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(150, "Receiving a file stream for " + path);
        con.receiveData(fs.writeFile(file, true));
        con.sendResponse(226, "File received!");
    }

    private void retr(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(150, "Sending the file stream for " + path + " (" + fs.getSize(file) + " bytes)");
        con.sendData(fs.readFile(file));
        con.sendResponse(226, "File sent!");
    }

    private void list(String path) throws IOException {
        con.sendResponse(150, "Sending file list...");

        String data = "";
        Object dir = path == null ? cwd : getFile(path);

        for(Object file : fs.listFiles(dir)) {
            data += Utils.format(fs, file);
        }

        con.sendData(data.getBytes("UTF-8"));
        con.sendResponse(226, "The list was sent");
    }

    private void nlst(String path) throws IOException {
        con.sendResponse(150, "Sending file list...");

        Object dir = path == null ? cwd : getFile(path);
        String data = "";

        for(Object file : fs.listFiles(dir)) {
            data += fs.getName(file) + "\r\n";
        }

        con.sendData(data.getBytes("UTF-8"));
        con.sendResponse(226, "The list was sent");
    }

    private void rmd(String path) throws IOException {
        Object file = getFile(path);
        
        if(!fs.isDirectory(file)) {
            con.sendResponse(550, "Not a directory");
            return;
        }

        fs.delete(file);
        con.sendResponse(250, '"' + path + '"' + " Directory Deleted");
    }

    private void dele(String path) throws IOException {
        Object file = getFile(path);

        if(fs.isDirectory(file)) {
            con.sendResponse(550, "Not a file");
            return;
        }

        fs.delete(file);
        con.sendResponse(250, '"' + path + '"' + " File Deleted");
    }

    private void mkd(String path) throws IOException {
        Object file = getFile(path);

        if(!fs.isDirectory(file)) {
            con.sendResponse(550, "Not a directory");
            return;
        }

        fs.mkdirs(file);
        con.sendResponse(257, '"' + path + '"' + " Directory Created");
    }

    private void mdtm(String path) throws IOException {
        Object file = getFile(path);

        Date date = new Date(fs.getLastModified(file));
        // TODO cache a SimpleDateFormat instance
        String time = new SimpleDateFormat("YYYYMMDDHHmmss", Locale.ENGLISH).format(date);

        con.sendResponse(213, time);
    }

    private void size(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(213, Long.toString(fs.getSize(file)));
    }

}
