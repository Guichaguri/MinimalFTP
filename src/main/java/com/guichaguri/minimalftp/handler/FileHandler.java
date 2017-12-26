/*
 * Copyright 2017 Guilherme Chaguri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guichaguri.minimalftp.handler;

import com.guichaguri.minimalftp.FTPConnection;
import com.guichaguri.minimalftp.Utils;
import com.guichaguri.minimalftp.api.IFileSystem;
import com.guichaguri.minimalftp.api.ResponseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.UUID;
import javax.xml.bind.DatatypeConverter;

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
    private long start = 0;

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
        con.registerCommand("REST", "REST <bytes>", this::rest); // Restart from a position
        con.registerCommand("ABOR", "ABOR", this::abor); // Abort all data transfers
        con.registerCommand("ALLO", "ALLO <size>", this::allo); // Allocate Space (Obsolete)
        con.registerCommand("RNFR", "RNFR <file>", this::rnfr); // Rename From
        con.registerCommand("RNTO", "RNTO <file>", this::rnto); // Rename To
        con.registerCommand("SMNT", "SMNT <file>", this::smnt); // Structure Mount (Obsolete)

        con.registerSiteCommand("CHMOD", "CHMOD <perm> <file>", this::site_chmod); // Change Permissions

        con.registerCommand("MDTM", "MDTM <file>", this::mdtm); // Modification Time (RFC 3659)
        con.registerCommand("SIZE", "SIZE <file>", this::size); // File Size (RFC 3659)
        con.registerCommand("MLST", "MLST <file>", this::mlst); // File Information (RFC 3659)
        con.registerCommand("MLSD", "MLSD <file>", this::mlsd); // List Files Information (RFC 3659)

        con.registerCommand("XCWD", "XCWD <file>", this::cwd); // Change Working Directory (RFC 775) (Obsolete)
        con.registerCommand("XCUP", "XCUP", this::cdup); // Change to Parent Directory (RFC 775) (Obsolete)
        con.registerCommand("XPWD", "XPWD", this::pwd); // Retrieve Working Directory (RFC 775) (Obsolete)
        con.registerCommand("XMKD", "XMKD <file>", this::mkd); // Create Directory (RFC 775) (Obsolete)
        con.registerCommand("XRMD", "XRMD <file>", this::rmd); // Delete Directory (RFC 775) (Obsolete)

        con.registerCommand("MFMT", "MFMT <time> <file>", this::mfmt); // Change Modified Time (draft-somers-ftp-mfxx-04)

        con.registerCommand("MD5", "MD5 <file>", this::md5); // MD5 Digest (draft-twine-ftpmd5-00) (Obsolete)

        con.registerCommand("HASH", "HASH <file>", this::hash); // Hash Digest (draft-bryan-ftpext-hash-02)

        con.registerFeature("base"); // Base Commands (RFC 5797)
        con.registerFeature("hist"); // Obsolete Commands (RFC 5797)
        con.registerFeature("REST STREAM"); // Restart in stream mode (RFC 3659)
        con.registerFeature("MDTM"); // Modification Time (RFC 3659)
        con.registerFeature("SIZE"); // File Size (RFC 3659)
        con.registerFeature("MLST Type*;Size*;Modify*;Perm*;"); // File Information (RFC 3659)
        con.registerFeature("TVFS"); // TVFS Mechanism (RFC 3659)
        con.registerFeature("MFMT"); // Change Modified Time (draft-somers-ftp-mfxx-04)
        con.registerFeature("MD5"); // MD5 Digest (draft-twine-ftpmd5-00)
        con.registerFeature("HASH MD5;SHA-1;SHA-256"); // Hash Digest (draft-bryan-ftpext-hash-02)

        con.registerOption("MLST", "Type;Size;Modify;Perm;");
        con.registerOption("HASH", "MD5");
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
        Object dir = getFile(path);

        if(fs.isDirectory(dir)) {
            cwd = dir;
            con.sendResponse(250, "The working directory was changed");
        } else {
            con.sendResponse(550, "Not a valid directory");
        }
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
        // Obsolete command. Accepts the command but takes no action
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

        receiveStream(fs.writeFile(file, start));
        start = 0;
    }

    private void stou(String[] args) throws IOException {
        Object file = null;
        String ext = ".tmp";

        if(args.length > 0) {
            file = getFile(args[0]);
            int i = args[0].lastIndexOf('.');
            if(i > 0) ext = args[0].substring(i);
        }

        while(file != null && fs.exists(file)) {
            // Quick way to generate simple random names
            // It's not the "perfect" solution, as it only uses hexadecimal characters
            // But definitely enough for file names
            String name = UUID.randomUUID().toString().replace("-", "");
            file = fs.findFile(cwd, name + ext);
        }

        con.sendResponse(150, "File: " + fs.getPath(file));
        receiveStream(fs.writeFile(file, 0));
    }

    private void appe(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(150, "Receiving a file stream for " + path);
        receiveStream(fs.writeFile(file, fs.exists(file) ? fs.getSize(file) : 0));
    }

    private void retr(String path) throws IOException {
        Object file = getFile(path);

        con.sendResponse(150, "Sending the file stream for " + path + " (" + fs.getSize(file) + " bytes)");
        sendStream(Utils.readFileSystem(fs, file, start, con.isAsciiMode()));
        start = 0;
    }

    private void rest(String byteStr) {
        long bytes = Long.parseLong(byteStr);
        if(bytes >= 0) {
            start = bytes;
            con.sendResponse(350, "Restarting at " + bytes + ". Ready to receive a RETR or STOR command");
        } else {
            con.sendResponse(501, "The number of bytes should be greater or equal to 0");
        }
    }

    private void abor() throws IOException {
        con.abortDataTransfers();
        con.sendResponse(226, "All transfers were aborted successfully");
    }

    private void list(String[] args) throws IOException {
        con.sendResponse(150, "Sending file list...");

        // "-l" is not present in any specification, but chrome uses it
        // TODO remove this when the bug gets fixed
        // https://bugs.chromium.org/p/chromium/issues/detail?id=706905
        Object dir = args.length > 0 && !args[0].equals("-l") ? getFile(args[0]) : cwd;

        if(!fs.isDirectory(dir)) {
            con.sendResponse(550, "Not a directory");
            return;
        }

        StringBuilder data = new StringBuilder();

        for(Object file : fs.listFiles(dir)) {
            data.append(Utils.format(fs, file));
        }

        con.sendData(data.toString().getBytes("UTF-8"));
        con.sendResponse(226, "The list was sent");
    }

    private void nlst(String[] args) throws IOException {
        con.sendResponse(150, "Sending file list...");

        // "-l" is not present in any specification, but chrome uses it
        // TODO remove this when the bug gets fixed
        // https://bugs.chromium.org/p/chromium/issues/detail?id=706905
        Object dir = args.length > 0 && !args[0].equals("-l") ? getFile(args[0]) : cwd;

        if(!fs.isDirectory(dir)) {
            con.sendResponse(550, "Not a directory");
            return;
        }

        StringBuilder data = new StringBuilder();

        for(Object file : fs.listFiles(dir)) {
            data.append(fs.getName(file)).append("\r\n");
        }

        con.sendData(data.toString().getBytes("UTF-8"));
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

    private void smnt() {
        // Obsolete command. The server should respond with a 502 code
        con.sendResponse(502, "SMNT is not implemented in this server");
    }

    private void site_chmod(String[] cmd) throws IOException {
        if(cmd.length <= 1) {
            con.sendResponse(501, "Missing parameters");
            return;
        }

        fs.chmod(getFile(cmd[1]), Utils.fromOctal(cmd[0]));
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

    private void mlst(String[] args) throws IOException {
        Object file = args.length > 0 ? getFile(args[0]) : cwd;

        if(!fs.exists(file)) {
            con.sendResponse(550, "File not found");
            return;
        }

        String[] options = con.getOption("MLST").split(";");
        String facts = Utils.getFacts(fs, file, options);

        con.sendResponse(250, "- Listing " + fs.getName(file) + "\r\n" + facts);
        con.sendResponse(250, "End");
    }

    private void mlsd(String[] args) throws IOException {
        Object file = args.length > 0 ? getFile(args[0]) : cwd;

        if(!fs.isDirectory(file)) {
            con.sendResponse(550, "Not a directory");
            return;
        }

        con.sendResponse(150, "Sending file information list...");

        String[] options = con.getOption("MLST").split(";");
        StringBuilder data = new StringBuilder();

        for(Object f : fs.listFiles(file)) {
            data.append(Utils.getFacts(fs, f, options));
        }

        con.sendData(data.toString().getBytes("UTF-8"));
        con.sendResponse(226, "The file list was sent!");
    }

    private void mfmt(String[] args) throws IOException {
        if(args.length < 2) {
            con.sendResponse(501, "Missing arguments");
            return;
        }

        Object file = getFile(args[1]);
        long time;

        if(!fs.exists(file)) {
            con.sendResponse(550, "File not found");
            return;
        }

        try {
            time = Utils.fromMdtmTimestamp(args[0]);
        } catch(ParseException ex) {
            con.sendResponse(500, "Couldn't parse the time");
            return;
        }

        fs.touch(file, time);
        con.sendResponse(213, "Modify=" + args[0] + "; " + fs.getPath(file));
    }

    private void md5(String path) throws IOException {
        try {
            Object file = getFile(path);
            byte[] digest = fs.getDigest(file, "MD5");
            String md5 = DatatypeConverter.printHexBinary(digest);

            con.sendResponse(251, fs.getName(file) + " " + md5);
        } catch(NoSuchAlgorithmException ex) {
            // Shouldn't ever happen
            con.sendResponse(504, ex.getMessage());
        }
    }

    private void hash(String path) throws IOException {
        try {
            Object file = getFile(path);
            String hash = con.getOption("HASH");
            byte[] digest = fs.getDigest(file, hash);
            String hex = DatatypeConverter.printHexBinary(digest);

            // TODO RANG
            con.sendResponse(213, String.format("%s 0-%s %s %s", hash, fs.getSize(file), hex, fs.getName(file)));
        } catch(NoSuchAlgorithmException ex) {
            con.sendResponse(504, ex.getMessage());
        }
    }

    /**
     * Sends a stream asynchronously, sending a response after it's done
     * @param in The stream
     */
    private void sendStream(InputStream in) {
        new Thread(() -> {
            try {
                con.sendData(in);
                con.sendResponse(226, "File sent!");
            } catch(ResponseException ex) {
                con.sendResponse(ex.getCode(), ex.getMessage());
            } catch(Exception ex) {
                con.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    /**
     * Receives a stream asynchronously, sending a response after it's done
     * @param out The stream
     */
    private void receiveStream(OutputStream out) {
        new Thread(() -> {
            try {
                con.receiveData(out);
                con.sendResponse(226, "File received!");
            } catch(ResponseException ex) {
                con.sendResponse(ex.getCode(), ex.getMessage());
            } catch(Exception ex) {
                con.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

}
