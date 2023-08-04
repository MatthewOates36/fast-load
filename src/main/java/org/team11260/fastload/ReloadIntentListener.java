package org.team11260.fastload;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.firstinspires.ftc.ftccommon.external.OnCreate;
import org.firstinspires.ftc.ftccommon.external.OnDestroy;
import org.firstinspires.ftc.robotcore.internal.opmode.AnnotatedOpModeClassFilter;
import org.firstinspires.ftc.robotcore.internal.opmode.ClassManager;
import org.firstinspires.ftc.robotcore.internal.opmode.OnBotJavaHelper;
import org.firstinspires.ftc.robotcore.internal.opmode.RegisteredOpModes;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import dalvik.system.DexFile;

/**
 * Listens for the reload intent from the Gradle plugin and preforms the reload with the new bundle.
 */
@SuppressWarnings("unused")
public class ReloadIntentListener {

    private static final File FAST_LOAD_JAR = new File(AppUtil.FIRST_FOLDER, "FastLoadBundle.jar");

    // detects reload request sent by gradle, completes reload, then sets the result code to 1 to let gradle know reload was successful
    private static final BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadFastLoadJar();
            setResultCode(1);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @OnCreate
    public static void start(Context context) {
        AppUtil.getDefContext().registerReceiver(reloadReceiver, new IntentFilter("team11260.RELOAD_FAST_LOAD"));
    }

    @OnDestroy
    public static void stop(Context context) {
        AppUtil.getDefContext().unregisterReceiver(reloadReceiver);
    }

    /**
     * Preforms a reload of the new dex files then forces a rescan for annotations.
     */
    private static void reloadFastLoadJar() {
        try {
            ClassManager classManager = ClassManager.getInstance();

            // allow a process all classes call to occur in the ClassManager
            Field processAllClassesCalled = classManager.getClass().getDeclaredField("processAllClassesCalled");
            processAllClassesCalled.setAccessible(true);
            ((AtomicBoolean) Objects.requireNonNull(processAllClassesCalled.get(classManager))).set(false);
            processAllClassesCalled.setAccessible(false);

            OnBotJavaHelper fastLoadHelper = new OnBotJavaHelper() {

                private DexFile dexFile;

                {
                    // have to load dex file twice due to a bug the dex file in detecting reloads
                    try {
                        dexFile = new DexFile(FAST_LOAD_JAR.getPath());
                    } catch (IOException ignored) {
                        // should occur everytime due with no original dex file error
                    }
                    try {
                        dexFile = new DexFile(FAST_LOAD_JAR.getPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public ClassLoader createOnBotJavaClassLoader() {
                    // new classloader will be used by ClassManager to load items from the fast load jar
                    return new TeamCodeClassLoader(FAST_LOAD_JAR.getPath(), this.getClass().getClassLoader());
                }

                @Override
                public Collection<String> getOnBotJavaClassNames() {
                    // list of classes so new or renamed classes will be scanned
                    return Collections.list(dexFile.entries());
                }

                @Override
                public Collection<String> getExternalLibrariesClassNames() {
                    return new ArrayList<>();
                }

                @Override
                public boolean isExternalLibrariesError(NoClassDefFoundError e) {
                    return false;
                }
            };

            classManager.setOnBotJavaClassHelper(fastLoadHelper);

            classManager.processAllClasses();

            // force reload all Op Modes
            RegisteredOpModes.getInstance().registerAllOpModes(m -> {});

            // force send new Op Modes to driver station
            AnnotatedOpModeClassFilter.getInstance().filterExternalLibrariesClassesStart();
            RegisteredOpModes.getInstance().setExternalLibrariesChanged();
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
