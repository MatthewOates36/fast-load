package org.team11260.fastload;

import dalvik.system.PathClassLoader;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Custom class loader to use the fast load dex bundle to get TeamCode classes
 * and prevent TeamCode classes from loading from the original APK.
 */
class TeamCodeClassLoader extends PathClassLoader {

    private static final List<String> IGNORED_ANNOTATIONS = List.of(
            "com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType"
    );

    public TeamCodeClassLoader(String urls, ClassLoader defaultClassLoader) {
        super(urls, defaultClassLoader);
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // loads classes from the new dex first before looking in the APK
        Class<?> loadedClass = findLoadedClass(name);
        if (null == loadedClass) {
            try {
                Class<?> defaultClass = getParent().loadClass(name);
                if (!shouldFastLoadClass(defaultClass)) {
                    return defaultClass;
                }
                loadedClass = findClass(name);
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

    /**
     * Classes nested in an ignored class are also kept from the APK so their types match the APK class using them.
     */
    static boolean shouldFastLoadClass(Class<?> loadedClass) {
        for (Class<?> checkedClass = loadedClass; checkedClass != null; checkedClass = checkedClass.getEnclosingClass()) {
            for (Annotation annotation : checkedClass.getAnnotations()) {
                for (String ignored : IGNORED_ANNOTATIONS) {
                    if (annotation.annotationType().getName().contains(ignored)) {
                        return false;
                    }
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
