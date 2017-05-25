package guichaguri.minimalftp.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
     */
    boolean exists(F file);

    /**
     * Checks if the file is a directory
     * @param file The file object
     */
    boolean isDirectory(F file);

    /**
     * Gets the permission number
     * @param file The file object
     */
    int getPermissions(F file);

    /**
     * Gets the file size
     * @param file The file object
     */
    long getSize(F file);

    /**
     * Gets the modified time in millis
     * @param file The file object
     */
    long getLastModified(F file);

    /**
     * Gets the amount of hard links
     * @param file The file object
     */
    int getHardLinks(F file);

    /**
     * Gets the file name
     * @param file The file object
     */
    String getName(F file);

    /**
     * Gets the file owner
     * @param file The file object
     */
    String getOwner(F file);

    /**
     * Gets the file group
     * @param file The file object
     */
    String getGroup(F file);

    /**
     * Gets the parent directory of a file.
     * This method should check for file access permissions
     *
     * @param file The file object
     * @throws java.io.FileNotFoundException When there's no permission to access the file
     * @throws IOException When an error occurs
     */
    F getParent(F file) throws IOException;

    /**
     * Lists file names, including directories of a directory inside the file system
     * This method should check for file access permissions
     *
     * @param dir The directory file object
     * @throws IOException When an error occurs
     */
    F[] listFiles(F dir) throws IOException;

    /**
     * Finds a file based on the path
     * This method should check for file access permissions
     *
     * @param path The path
     * @throws java.io.FileNotFoundException When there's no permission to access the file or the file doesn't exist
     * @throws IOException When an error occurs
     */
    F findFile(String path) throws IOException;

    /**
     * Finds a file based on the path
     * This method should check for file access permissions
     *
     * @param cwd The base directory
     * @param path The path
     * @throws java.io.FileNotFoundException When there's no permission to access the file or the file doesn't exist
     * @throws IOException When an error occurs
     */
    F findFile(F cwd, String path) throws IOException;

    /**
     * Reads a file into an input stream
     * @param file The file object
     * @throws IOException When an error occurs
     */
    InputStream readFile(F file) throws IOException;

    /**
     * Writes a file into an output stream
     * If the file does not exist, creates the file
     *
     * @param file The file object
     * @param append Whether the bytes should be written at the end of the file
     * @throws IOException When an error occurs
     */
    OutputStream writeFile(F file, boolean append) throws IOException;

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

}
