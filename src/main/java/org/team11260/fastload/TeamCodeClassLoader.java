package org.team11260.fastload;

import dalvik.system.PathClassLoader;

/**
 * Custom class loader to use the fast load dex bundle to get TeamCode classes
 * and prevent TeamCode classes from loading from the original APK.
 */
class TeamCodeClassLoader extends PathClassLoader {

    public TeamCodeClassLoader(String urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // loads classes from the new dex first before looking in the APK
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
                if (name.contains("org.firstinspires.ftc.teamcode")) {
                    // prevents classes that were deleted or renamed in fast load from being loaded from original APK
                    return RemovedTeamCodeClass.class;
                }
                loadedClass = super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }

    /**
     * Placeholder class for team code classes removed in a fast load
     */
    private static final class RemovedTeamCodeClass {

    }
}
