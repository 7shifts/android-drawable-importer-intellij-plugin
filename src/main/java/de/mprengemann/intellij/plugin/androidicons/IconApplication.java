package de.mprengemann.intellij.plugin.androidicons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.util.io.ZipUtil;
import de.mprengemann.intellij.plugin.androidicons.controllers.DefaultControllerFactory;
import de.mprengemann.intellij.plugin.androidicons.controllers.IControllerFactory;
import de.mprengemann.intellij.plugin.androidicons.model.IconPack;
import de.mprengemann.intellij.plugin.androidicons.model.Resolution;
import de.mprengemann.intellij.plugin.androidicons.resources.ResourceLoader;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;

public class IconApplication implements ApplicationComponent {

    private static final String TAG = IconApplication.class.getSimpleName();
    private static final Logger LOGGER = Logger.getInstance(TAG);
    private IControllerFactory controllerFactory;

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getName();
    }

    @Override
    public void initComponent() {
        IconPack androidIcons = null;
        IconPack materialIcons = null;
        try {
            // Instantiate the bundled IconPacks to be passed to the DefaultControllerFactory
            // This reads "content.json" and ALWAYS gets done.
            final BufferedReader fileReader = ResourceLoader.getBundledResReader("content.json");
            assert fileReader != null;
            final Type listType = new TypeToken<ArrayList<IconPack>>() {}.getType();
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Resolution.class, new Resolution.Deserializer());
            final Gson gson = gsonBuilder.create();
            final List<IconPack> iconPacks = gson.fromJson(fileReader, listType);
            androidIcons = iconPacks.get(0);    LOGGER.info(format("%d androidIcons", androidIcons.getAssets().size()));
            materialIcons = iconPacks.get(1);   LOGGER.info(format("%d materialIcons", materialIcons.getAssets().size()));

            // Get the bundled property file (/assets) ...
            String propFn = "icon_packs.properties";
            Properties bundledIconPackProperties = new Properties();
            bundledIconPackProperties.load(ResourceLoader.getBundledResourceStream(propFn));
            // ... and compare 'version' with the local property file (/)
            boolean export;
            File propFile = new File(ResourceLoader.getExportPath(), propFn);
            // NB: Above line may create /home/[userId]/.AndroidStudio3.6/system/ADI-hack-export
            LOGGER.info(format("propFile.exists()=%b\n%s", propFile.exists(), propFile.getAbsolutePath()));
            if (!propFile.exists()) {
                export = true;
            } else {
                Properties localIconPackProperties = new Properties();
                localIconPackProperties.load(FileUtils.openInputStream(propFile));
                export = Integer.parseInt(bundledIconPackProperties.getProperty("version")) !=
                         Integer.parseInt(localIconPackProperties.getProperty("version"));
            }

            if (export) {
                LOGGER.info("Preparing Android Drawable Importer");
                new Task.Modal(null, "Prepare Android Drawable Importer", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        progressIndicator.setIndeterminate(true);

                        // Extract the property file from the jar
                        final File localProp = ResourceLoader.getFromJar(propFn);
                        assert localProp != null;

                        // "icon_packs.zip" contains: android_icons, material_icons, __MACOSX
                        final File tempIconZip = ResourceLoader.getFromJar("icon_packs.zip");
                        assert tempIconZip != null;
                        try { ZipUtil.extract(tempIconZip, ResourceLoader.getExportPath(), null, true); }
                        catch (IOException e) { LOGGER.error(e); }
                        tempIconZip.delete();       // No longer required
                        LOGGER.info("Icon packs prepared. Android Drawable Importer installed.");
                    }
                }.queue();
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        controllerFactory = new DefaultControllerFactory(androidIcons, materialIcons);
    }

    @Override
    public void disposeComponent() {
        controllerFactory.tearDown();
        controllerFactory = null;
    }

    public IControllerFactory getControllerFactory() {
        return controllerFactory;
    }
}