package org.team11260.fastload;

import dalvik.system.PathClassLoader;

class TeamCodeClassLoader extends PathClassLoader {

    public TeamCodeClassLoader(String urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
                if (name.contains("org.firstinspires.ftc.teamcode")) {
                    // prevents classes that were deleted or renamed in fast load from being loaded from original apk
                    throw new ClassNotFoundException();
                }
                loadedClass = super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }
}
