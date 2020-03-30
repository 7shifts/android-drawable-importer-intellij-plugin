package de.mprengemann.intellij.plugin.androidicons.resources;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtilRt;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static java.lang.String.format;

public class ResourceLoader {

    private static final String TAG = ResourceLoader.class.getSimpleName();
    private static final Logger LOGGER = Logger.getInstance(TAG);

    static ResourceLoader rl = new ResourceLoader();

    /**  */
    public static File getExportPath() {
        final String exportPath = PathManager.getSystemPath();
        File export = new File(exportPath, "ADI-hack-export");
        if (!export.exists()) {
            LOGGER.info(format("Creating export folder:\n%s", export.getAbsolutePath()));
            FileUtilRt.createDirectory(export);
        }
        return export;
    }

    public static File getAssetResource(String file) {
        return new File(getExportPath(), file);
    }

    /** Discontinued: No longer handles Exclamation mark in paths like <br>
    /home/daz/.AndroidStudio3.6/config/plugins/ADI-hack/lib/ADI-hack-0.7.jar!/assets/icon_packs.zip */
    public static File getBundledResource(String file) {
        final URL resource = rl.getClass().getResource(getAssetPath(file));
        if (resource == null) {
            return null;
        }
        try {
            LOGGER.info("Matt_"+resource.getPath());
            // java.lang.IllegalArgumentException: URI is not hierarchical
            return new File(resource.toURI());
        } catch (URISyntaxException e) {
            LOGGER.error(e);
            return new File(resource.getPath());
        }
    }

    /** Untested. Superceded by {@link #getFromJar(String)} */
    public static File getBundledResFile(String file) {
        BufferedReader reader = getBundledResReader(file);
        if (reader==null) return null;
        File out = new File(getExportPath(), file);
        LOGGER.info("Local output file: " + out.getAbsolutePath());
        out.delete();
        int count=0;
        try {
            out.createNewFile();
            FileWriter fileWriter = new FileWriter(out, false);
            String line;
            while ((line = reader.readLine()) != null) {
                fileWriter.write(line);
                count++;
            }
            fileWriter.close();
        } catch (IOException ioe) {
            LOGGER.error(format("Count=%d IOE: %s", count, ioe.getMessage()));
        }
        return out;
    }

    /** Extract the named file from the plugin distribution .jar <br>
    // https://stackoverflow.com/a/44077426/2376004
    @param      fn      File to be extracted
    @return     File instance describing output file or null */
    public static File getFromJar(String fn) {
        InputStream is = getBundledResourceStream(fn);
        if (is==null) return null;
        String out = new File(getExportPath(), fn).getAbsolutePath();
        LOGGER.info(format("Copying -> %s \n\tto -> %s", is.toString(), out));
        try { Files.copy(is, Paths.get(out), StandardCopyOption.REPLACE_EXISTING); }
        catch (NoSuchFileException nsf) { LOGGER.error("getZip_nsf: " + nsf.getMessage()); return null; }
        catch (IOException ioe) { LOGGER.error("getZip_ioe: " + ioe.getMessage()); return null; }
        catch (Exception e) { LOGGER.error(e); return null; }
        return new File(out);
    }

    public static BufferedReader getBundledResReader(String file) {
        InputStream in = getBundledResourceStream(file);
        if (in==null) return null;
        LOGGER.info(format("Returning BufferedReader for %s", getAssetPath(file)));
        return new BufferedReader(new InputStreamReader(in));
    }

    public static InputStream getBundledResourceStream(String file) {
        final URL resource = rl.getClass().getResource(getAssetPath(file));
        if (resource == null) { return null; }
        LOGGER.info(format("Returning InputStream for %s", resource.getPath()));
        return rl.getClass().getResourceAsStream(getAssetPath(file));
    }

    @NotNull
    private static String getAssetPath(String file) {
        return format("/assets/%s", file);
    }

}
