package com.windowmirror.android.util;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class for accessing the internal file system.
 * @author alliecurry
 */
public final class FileUtility {

    private FileUtility() {
        throw new AssertionError();
    }

    /** @return the absolute path to the directory where this app will store files. */
    public static String getDirectoryPath() {
        String path =  Environment.getExternalStorageDirectory().getAbsolutePath();
        return path + "/WindowMirror";
    }

    /**
     * Creates the directory where files are to be locally stored.
     * @return true if the directory was successfully created or if it already exists.
     */
    public static boolean makeDirectories() {
        final File f = new File(getDirectoryPath());
        return f.exists() || f.mkdirs();
    }

    /** @return String to use as a file path for a new audio recording.*/
    public static String generateAudioFileName() {
        final String uniqueName = new SimpleDateFormat("yy-MM-dd-k-m-s", Locale.US).format(new Date());
        return "audio-" + uniqueName;
    }

    public static void copyFile(final File source, final File dest) throws IOException {
        FileChannel sourceChannel = new FileInputStream(source).getChannel();
        FileChannel destChannel = new FileOutputStream(dest).getChannel();
        destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        sourceChannel.close();
        destChannel.close();
    }
}
