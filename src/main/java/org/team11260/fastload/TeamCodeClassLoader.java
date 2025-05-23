package org.team11260.fastload;

import dalvik.system.PathClassLoader;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Custom class loader to use the fast load dex bundle to get TeamCode classes
 * and prevent TeamCode classes from loading from the original APK.
 */
class TeamCodeClassLoader extends PathClassLoader {

    private final List<String> ignoredAnnotations = List.of(
            "com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType"
    );

    private final ClassLoader parent;

    public TeamCodeClassLoader(String urls, ClassLoader parent) {
        super(urls, parent);
        this.parent = parent;
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // loads classes from the new dex first before looking in the APK
        Class<?> loadedClass = findLoadedClass(name);
        if (null == loadedClass) {
            try {
                loadedClass = findClass(name);
                if (!shouldFastLoadClass(loadedClass)) {
                    return parent.loadClass(name);
                }
            } catch (ClassNotFoundException e) {
                if (name.contains("org.firstinspires.ftc.teamcode")) {
                    // prevents classes that were deleted or renamed in fast load from being loaded from original APK
                    return RemovedTeamCodeClass.class;
                }
                return super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }

    private boolean shouldFastLoadClass(Class<?> loadedClass) {
        for (Annotation annotation : loadedClass.getAnnotations()) {
            for (String ignored : ignoredAnnotations) {
                if (annotation.annotationType().getName().contains(ignored)) {
                    System.out.println("Ignored TeamCode class: " + loadedClass);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Placeholder class for team code classes removed in a fast load
     */
    private static final class RemovedTeamCodeClass {
    }
}
