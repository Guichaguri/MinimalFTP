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

package com.guichaguri.minimalftp.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a File System
 * @author Guilherme Chaguri
 */
public interface IFileSystem<F extends Object> {

    /**
     * Retrieves the root file object
     * @return The file object
     */
    F getRoot();

    /**
     * Gets the relative path of a file from the file system's root
     * @param file The file object
     * @return The relative path
     */
    String getPath(F file);

    /**
     * Gets whether the file exists
     * @param file The file object
     * @return {@code true} if the file exists
     */
    boolean exists(F file);

    /**
     * Checks if the file is a directory
     * @param file The file object
     * @return {@code true} if the file is a directory
     */
    boolean isDirectory(F file);

    /**
     * Gets the permission number
     * @param file The file object
     * @return The octal permission number in decimal
     */
    int getPermissions(F file);

    /**
     * Gets the file size
     * @param file The file object
     * @return The file size in bytes
     */
    long getSize(F file);

    /**
     * Gets the modified time.
     * @param file The file object
     * @return The modified time in millis
     */
    long getLastModified(F file);

    /**
     * Gets the amount of hard links.
     * @param file The file object
     * @return The number of hard links
     */
    int getHardLinks(F file);

    /**
     * Gets the file name
     * @param file The file object
     * @return The file name
     */
    String getName(F file);

    /**
     * Gets the file owner
     * @param file The file object
     * @return The owner name
     */
    String getOwner(F file);

    /**
     * Gets the file group
     * @param file The file object
     * @return The group name
     */
    String getGroup(F file);

    /**
     * Gets (or calculates) the hash digest of a file.
     *
     * The algorithms "MD5", "SHA-1" and "SHA-256" are required to be implemented
     *
     * @param file The file object
     * @param algorithm The digest algorithm
     * @return The hash digest
     * @throws NoSuchAlgorithmException When the algorithm is not implement
     * @throws IOException When an error occurs
     */
    default byte[] getDigest(F file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest d = MessageDigest.getInstance(algorithm);
        InputStream in = readFile(file, 0);
        byte[] bytes = new byte[1024];
        int length;

        while((length = in.read(bytes)) != -1) {
            d.update(bytes, 0, length);
        }

        return d.digest();
    }

    /**
     * Gets the parent directory of a file.
     *
     * This method should check for file access permissions
     *
     * @param file The file object
     * @return The parent file
     * @throws java.io.FileNotFoundException When there's no permission to access the file
     * @throws IOException When an error occurs
     */
    F getParent(F file) throws IOException;

    /**
     * Lists file names, including directories of a directory inside the file system.
     *
     * This method should check for file access permissions
     *
     * @param dir The directory file object
     * @return A file array
     * @throws IOException When an error occurs
     */
    F[] listFiles(F dir) throws IOException;

    /**
     * Finds a file based on the path.
     *
     * This method should check for file access permissions
     *
     * @param path The path
     * @return The found file
     * @throws java.io.FileNotFoundException When there's no permission to access the file or the file doesn't exist
     * @throws IOException When an error occurs
     */
    F findFile(String path) throws IOException;

    /**
     * Finds a file based on the path.
     *
     * This method should check for file access permissions
     *
     * @param cwd The base directory
     * @param path The path
     * @return The found file
     * @throws java.io.FileNotFoundException When there's no permission to access the file or the file doesn't exist
     * @throws IOException When an error occurs
     */
    F findFile(F cwd, String path) throws IOException;

    /**
     * Reads a file into an input stream
     * @param file The file object
     * @param start The position in bytes to start reading from
     * @return The input stream of the file
     * @throws IOException When an error occurs
     */
    InputStream readFile(F file, long start) throws IOException;

    /**
     * Writes a file into an output stream.
     *
     * If the file does not exist, creates the file
     *
     * @param file The file object
     * @param start The position in bytes to start writing to
     * @return The output stream of the file
     * @throws IOException When an error occurs
     */
    OutputStream writeFile(F file, long start) throws IOException;

    /**
     * Creates a directory
     * @param file The file object
     * @throws IOException When an error occurs
     */
    void mkdirs(F file) throws IOException;

    /**
     * Deletes a file
     * @param file The file object
     * @throws IOException When an error occurs
     */
    void delete(F file) throws IOException;

    /**
     * Renames or moves a file
     * @param from The original file
     * @param to The destination
     * @throws IOException When an error occurs
     */
    void rename(F from, F to) throws IOException;

    /**
     * Changes the permissions of a file
     * @param file The file object
     * @param perms The permissions number
     * @throws IOException When an error occurs
     */
    void chmod(F file, int perms) throws IOException;

    /**
     * Updates the modified time of a file
     * @param file The file object
     * @param time The new time in milliseconds
     * @throws IOException When an error occurs
     */
    void touch(F file, long time) throws IOException;

}
