package guichaguri.minimalftp.handler;

import guichaguri.minimalftp.FTPConnection;
import guichaguri.minimalftp.Utils;
import guichaguri.minimalftp.api.IFileSystem;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles file management commands
 * @author Guilherme Chaguri
 */
@SuppressWarnings("unchecked")
public class FileHandler {

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

    public void registerCommands() {
        con.registerCommand("CWD", "CWD <file>", this::cwd); // Change Working Directory
        con.registerCommand("CDUP", "CDUP", this::cdup); // Change to Parent Directory
        con.registerCommand("PWD", "PWD", this::pwd); // Retrieve Working Directory
        con.registerCommand("MKD", "MKD <file>", this::mkd); // Create Directory
        con.registerCommand("RMD", "RMD <file>", this::rmd); // Delete Directory
        con.registerCommand("DELE", "DELE <file>", this::dele); // Delete File
        con.registerCommand("LIST", "LIST [file]", this::list); // List Files
        con.registerCommand("NLST", "NLST [file]", this::nlst); // List File Names
        con.registerCommand("RETR", "RETR <file>", this::retr); // Retrieve File
        con.registerCommand("STOR", "STOR <file>", this::stor); // Store File
        con.registerCommand("STOU", "STOU [file]", this::stou); // Store Random File
        con.registerCommand("APPE", "APPE <file>", this::appe); // Append File
        con.registerCommand("ALLO", "ALLO <size>", this::allo); // Allocate Space
        con.registerCommand("RNFR", "RNFR <file>", this::rnfr); // Rename From
        con.registerCommand("RNTO", "RNTO <file>", this::rnto); // Rename To
        con.registerCommand("SITE", "SITE <cmd>", this::site); // Special Commands

        con.registerCommand("MDTM", "MDTM <file>", this::mdtm); // Modification Time (RFC 3659)
        con.registerCommand("SIZE", "SIZE <file>", this::size); // File Size (RFC 3659)

        con.registerCommand("XCWD", "XCWD <file>", this::cwd); // Change Working Directory (RFC 775)
        con.registerCommand("XCUP", "XCUP", this::cdup); // Change to Parent Directory (RFC 775)
        con.registerCommand("XPWD", "XPWD", this::pwd); // Retrieve Working Directory (RFC 775)
        con.registerCommand("XMKD", "XMKD <file>", this::mkd); // Create Directory (RFC 775)
        con.registerCommand("XRMD", "XRMD <file>", this::rmd); // Delete Directory (RFC 775)
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
        if(rnFile == null) {
            con.sendResponse(503, "No rename request was received");
            return;
        }

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

    private void stou(String[] args) throws IOException {
        Object file = null;
        String ext = ".tmp";

        if(args.length > 1) {
            file = getFile(args[1]);
            int i = args[1].lastIndexOf('.');
            if(i > 0) ext = args[1].substring(i);
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

    private void list(String[] args) throws IOException {
        con.sendResponse(150, "Sending file list...");

        Object dir = args.length > 1 ? getFile(args[1]) : cwd;
        String data = "";

        for(Object file : fs.listFiles(dir)) {
            data += Utils.format(fs, file);
        }

        con.sendData(data.getBytes("UTF-8"));
        con.sendResponse(226, "The list was sent");
    }

    private void nlst(String[] args) throws IOException {
        con.sendResponse(150, "Sending file list...");

        Object dir = args.length > 1 ? getFile(args[1]) : cwd;
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

        fs.mkdirs(file);
        con.sendResponse(257, '"' + path + '"' + " Directory Created");
    }

    private void site(String[] args) throws IOException {
        String cmd = args.length > 1 ? args[1].toUpperCase() : null;

        if(cmd == null) {
            con.sendResponse(500, "Missing the command name");
            return;
        } else if(cmd.equals("CHMOD")) {
            if(args.length > 2) {
                site_chmod(args[2], args[3]);
                return;
            }
        } else {
            con.sendResponse(504, "Unknown site command");
            return;
        }
        con.sendResponse(501, "Missing parameters");
    }

    private void site_chmod(String perm, String path) throws IOException {
        fs.chmod(getFile(path), Utils.fromOctal(perm));
        con.sendResponse(200, "The file permissions were successfully changed");
    }

    private void mdtm(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(213, Utils.toMdtmTimestamp(fs.getLastModified(file)));
    }

    private void size(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(213, Long.toString(fs.getSize(file)));
    }

}
