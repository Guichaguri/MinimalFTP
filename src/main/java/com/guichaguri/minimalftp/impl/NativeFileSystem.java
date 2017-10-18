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

package com.guichaguri.minimalftp.impl;

import com.guichaguri.minimalftp.api.IFileSystem;
import com.guichaguri.minimalftp.Utils;
import java.io.*;

/**
 * Native File System
 *
 * Allows the manipulation of any file inside a directory
 * @author Guilherme Chaguri
 */
public class NativeFileSystem implements IFileSystem<File> {

    private final File rootDir;

    /**
     * Creates a native file system.
     *
     * If the root directory does not exists, it will be created
     * @param rootDir The root directory
     */
    public NativeFileSystem(File rootDir) {
        this.rootDir = rootDir;

        if(!rootDir.exists()) rootDir.mkdirs();
    }

    @Override
    public File getRoot() {
        return rootDir;
    }

    @Override
    public String getPath(File file) {
        return rootDir.toURI().relativize(file.toURI()).getPath();
    }

    @Override
    public boolean exists(File file) {
        return file.exists();
    }

    @Override
    public boolean isDirectory(File file) {
        return file.isDirectory();
    }

    @Override
    public int getPermissions(File file) {
        int perms = 0;
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_READ, file.canRead());
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_WRITE, file.canWrite());
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_EXECUTE, file.canExecute());
        return perms;
    }

    @Override
    public long getSize(File file) {
        return file.length();
    }

    @Override
    public long getLastModified(File file) {
        return file.lastModified();
    }

    @Override
    public int getHardLinks(File file) {
        return file.isDirectory() ? 3 : 1;
    }

    @Override
    public String getName(File file) {
        return file.getName();
    }

    @Override
    public String getOwner(File file) {
        return "-";
    }

    @Override
    public String getGroup(File file) {
        return "-";
    }

    @Override
    public File getParent(File file) throws IOException {
        if(file.equals(rootDir)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file.getParentFile();
    }

    @Override
    public File[] listFiles(File dir) throws IOException {
        if(!dir.isDirectory()) throw new IOException("Not a directory");

        return dir.listFiles();
    }

    @Override
    public File findFile(String path) throws IOException {
        File file = new File(rootDir, path);

        if(!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file;
    }

    @Override
    public File findFile(File cwd, String path) throws IOException {
        File file = new File(cwd, path);

        if(!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file;
    }

    @Override
    public InputStream readFile(File file, long start) throws IOException {
        // Not really needed, but helps a bit in performance
        if(start <= 0) {
            return new FileInputStream(file);
        }

        // Use RandomAccessFile to seek a file
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(start);

        // Create a stream using the RandomAccessFile
        return new FileInputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public OutputStream writeFile(File file, long start) throws IOException {
        // Not really needed, but helps a bit in performance
        if(start <= 0) {
            return new FileOutputStream(file, false);
        } else if(start == file.length()) {
            return new FileOutputStream(file, true);
        }

        // Use RandomAccessFile to seek a file
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(start);

        // Create a stream using the RandomAccessFile
        return new FileOutputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public void mkdirs(File file) throws IOException {
        if(!file.mkdirs()) throw new IOException("Couldn't create the directory");
    }

    @Override
    public void delete(File file) throws IOException {
        if(!file.delete()) throw new IOException("Couldn't delete the file");
    }

    @Override
    public void rename(File from, File to) throws IOException {
        if(!from.renameTo(to)) throw new IOException("Couldn't rename the file");
    }

    @Override
    public void chmod(File file, int perms) throws IOException {
        file.setReadable(Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_READ), true);
        file.setWritable(Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_WRITE), true);
        file.setExecutable(Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_EXECUTE), true);
    }

    @Override
    public void touch(File file, long time) throws IOException {
        if(!file.setLastModified(time)) throw new IOException("Couldn't touch the file");
    }

    private boolean isInside(File dir, File file) {
        if(file.equals(dir)) return true;

        try {
            return file.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator);
        } catch(IOException ex) {
            return false;
        }
    }

}
