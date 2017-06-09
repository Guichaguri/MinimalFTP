package com.guichaguri.minimalftp;

import com.guichaguri.minimalftp.api.IFileSystem;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.net.ssl.SSLContext;

/**
 * @author Guilherme Chaguri
 */
public class Utils {

    // Permission Categories
    public static final int CAT_OWNER = 6;
    public static final int CAT_GROUP = 3;
    public static final int CAT_PUBLIC = 0;

    // Permission Types
    public static final int TYPE_READ = 2;
    public static final int TYPE_WRITE = 1;
    public static final int TYPE_EXECUTE = 0;

    private static final SimpleDateFormat mdtmFormat = new SimpleDateFormat("YYYYMMDDHHmmss", Locale.ENGLISH);
    private static final SimpleDateFormat hourFormat = new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH);
    private static final SimpleDateFormat yearFormat = new SimpleDateFormat("MMM dd YYYY", Locale.ENGLISH);
    private static final long sixMonths = 183L * 24L * 60L * 60L * 1000L;

    public static String toUnixTimestamp(long time) {
        Date date = new Date(time);

        if(System.currentTimeMillis() - time > sixMonths) {
            return yearFormat.format(date);
        }
        return hourFormat.format(date);
    }

    public static String toMdtmTimestamp(long time) {
        return mdtmFormat.format(new Date(time));
    }

    public static long fromMdtmTimestamp(String time) throws ParseException {
        return mdtmFormat.parse(time).getTime();
    }

    public static <F> String format(IFileSystem<F> fs, F file) {
        // Intended Format
        // -rw-rw-rw-   1 owner   group    7045120 Aug 08  5:24 video.mp4
        // -rw-rw-rw-   1 owner   group        380 May 26 21:50 data.txt
        // drwxrwxrwx   3 owner   group          0 Oct 12  8:21 directory

        return String.format("%s %3d %-8s %-8s %8d %s %s\r\n",
                getPermission(fs, file),
                fs.getHardLinks(file),
                fs.getOwner(file),
                fs.getGroup(file),
                fs.getSize(file),
                toUnixTimestamp(fs.getLastModified(file)),
                fs.getName(file));
    }

    public static <F> String getPermission(IFileSystem<F> fs, F file) {
        String perm = "";
        int perms = fs.getPermissions(file);

        perm += fs.isDirectory(file) ? 'd' : '-';

        perm += hasPermission(perms, CAT_OWNER + TYPE_READ) ? 'r' : '-';
        perm += hasPermission(perms, CAT_OWNER + TYPE_WRITE) ? 'w' : '-';
        perm += hasPermission(perms, CAT_OWNER + TYPE_EXECUTE) ? 'x' : '-';

        perm += hasPermission(perms, CAT_GROUP + TYPE_READ) ? 'r' : '-';
        perm += hasPermission(perms, CAT_GROUP + TYPE_WRITE) ? 'w' : '-';
        perm += hasPermission(perms, CAT_GROUP + TYPE_EXECUTE) ? 'x' : '-';

        perm += hasPermission(perms, CAT_PUBLIC + TYPE_READ) ? 'r' : '-';
        perm += hasPermission(perms, CAT_PUBLIC + TYPE_WRITE) ? 'w' : '-';
        perm += hasPermission(perms, CAT_PUBLIC + TYPE_EXECUTE) ? 'x' : '-';

        return perm;
    }

    public static <F> String getFacts(IFileSystem<F> fs, F file, String[] options) {
        String facts = "";
        boolean dir = fs.isDirectory(file);

        for(String opt : options) {
            opt = opt.toLowerCase();

            if(opt.equals("modify")) {
                facts += "Modify=" + Utils.toMdtmTimestamp(fs.getLastModified(file)) + ";";
            } else if(opt.equals("size")) {
                facts += "Size=" + fs.getSize(file) + ";";
            } else if(opt.equals("type")) {
                facts += "Type=" + (dir ? "dir" : "file") + ";";
            } else if(opt.equals("perm")) {
                int perms = fs.getPermissions(file);
                String perm = "";

                if(hasPermission(perms, CAT_OWNER + TYPE_READ)) {
                    perm += dir ? "el" : "r";
                }
                if(hasPermission(perms, CAT_OWNER + TYPE_WRITE)) {
                    perm += "f";
                    perm += dir ? "pcm" : "adw";
                }

                facts += "Perm=" + perm + ";";
            }
        }

        facts += " " + fs.getPath(file) + "\r\n";
        return facts;
    }

    public static void write(OutputStream out, byte[] bytes, int len, boolean ascii) throws IOException {
        if(ascii) {
            // ASCII - Add \r before \n when necessary
            byte lastByte = 0;
            for(int i = 0; i < len; i++) {
                byte b = bytes[i];

                if(b == '\n' && lastByte != '\r') {
                    out.write('\r');
                }

                out.write(b);
                lastByte = b;
            }
        } else {
            // Binary - Keep all \r\n as is
            out.write(bytes, 0, len);
        }
    }

    public static <F> InputStream readFileSystem(IFileSystem<F> fs, F file, long start, boolean ascii) throws IOException {
        if(ascii) {
            InputStream in = new BufferedInputStream(fs.readFile(file, 0));
            long offset = 0;

            // Count \n as two bytes for skipping
            while(start >= offset++) {
                int c = in.read();
                if(c == -1) {
                    throw new IOException("Couldn't skip this file. End of the file was reached");
                } else if(c == '\n') {
                    offset++;
                }
            }

            return in;
        } else {
            return fs.readFile(file, start);
        }
    }

    public static boolean hasPermission(int perms, int perm) {
        return (perms >> perm & 1) == 1;
    }

    public static int setPermission(int perms, int perm, boolean b) {
        perm = 1 << perm;
        return b ? perms | perm : perms & ~perm;
    }

    public static int fromOctal(String perm) {
        return Integer.parseInt(perm, 8);
    }

    public static ServerSocket createServer(int port, int backlog, InetAddress address, SSLContext context, boolean ssl) throws IOException {
        if(ssl) {
            if(context == null) throw new NullPointerException("The SSL context is null");
            return context.getServerSocketFactory().createServerSocket(port, backlog, address);
        }
        return new ServerSocket(port, backlog, address);
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch(IOException e) {}
    }

}
