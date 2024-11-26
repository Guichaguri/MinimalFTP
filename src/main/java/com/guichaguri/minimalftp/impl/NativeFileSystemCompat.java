package com.guichaguri.minimalftp.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class NativeFileSystemCompat extends NativeFileSystem {

    private boolean isReadOnly;
    private String readOnlyMessage = "Permission denied. FTP server is configured for read-only access.";

    public NativeFileSystemCompat(File rootDir) {
        super(rootDir);
    }

    public NativeFileSystemCompat(File rootDir, boolean isReadOnly) {
        super(rootDir);
        this.isReadOnly = isReadOnly;
    }

    public NativeFileSystemCompat(File rootDir, boolean isReadOnly, String readOnlyMessage) {
        super(rootDir);
        this.isReadOnly = isReadOnly;
        this.readOnlyMessage = readOnlyMessage;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public void setReadOnlyMessage(String readOnlyMessage) {
        this.readOnlyMessage = readOnlyMessage;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public String getReadOnlyMessage() {
        return readOnlyMessage;
    }

    private void throwReadOnlyException() throws IOException {
        throw new IOException(readOnlyMessage);
    }

    // Files.walk() method requires API level 26 in Android
    // For compatibility with older versions (Java 7), we used file.listFiles() to delete
    @Override
    public void delete(File file) throws IOException {
        if (isReadOnly) throwReadOnlyException();

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    delete(childFile);
                }
            }
        }

        if (!file.delete())
            throw new IOException("Couldn't delete file: " + file.getAbsolutePath());
    }

    @Override
    public OutputStream writeFile(File file, long start) throws IOException {
        if (isReadOnly) throwReadOnlyException();
        return super.writeFile(file, start);
    }

    @Override
    public void mkdirs(File file) throws IOException {
        if (isReadOnly) throwReadOnlyException();
        super.mkdirs(file);
    }

    @Override
    public void rename(File from, File to) throws IOException {
        if (isReadOnly) throwReadOnlyException();
        super.rename(from, to);
    }

    @Override
    public void chmod(File file, int perms) throws IOException {
        if (isReadOnly) throwReadOnlyException();
        super.chmod(file, perms);
    }

    @Override
    public void touch(File file, long time) throws IOException {
        if (isReadOnly) throwReadOnlyException();
        super.touch(file, time);
    }
}
