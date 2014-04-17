/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.sabob.olive.loader;

import java.io.*;

/**
 *
 */
public class ClasspathResourceLoader implements ResourceLoader {

    private Class classLoader;

    public ClasspathResourceLoader() {
    }

    public ClasspathResourceLoader(Class classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public InputStream getResourceStream(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null!");
        }

        //String fullname = normalizeName(source, getRelativeLoader());
        Class loader = getClassLoader();

        InputStream is = loader.getResourceAsStream(source);

        if (is == null) {
            if (source.startsWith("/")) {
                String pkg = getPackage(source);

                throw new IllegalStateException("The absolute source '" + source + "' cannot be found in the package '" + pkg + "'!");
            } else {
                String pkg = getPackage(source);
                pkg = getClassLoader().getPackage().getName().replace(".", "/") + "/" + pkg;

                throw new IllegalStateException("The relative source '" + source + "' cannot be found in the package '" + pkg + "'!");
            }
        }
        return is;
    }
    
    public static String normalizeName(String filename, Class relative) {
        if (filename.startsWith("/")) {
            return filename;
        }

        String classPackage = relative.getPackage().getName();
        classPackage = classPackage.replace(".", "/");
        String fullname = "/" + classPackage + "/" + filename;
        return fullname;
    }

    protected String getPackage(String filename) {
        int index = filename.lastIndexOf("/");
        String pkg = filename;
        if (index >= 0) {
            pkg = filename.substring(0, index);
        }
        return pkg;
    }

    /**
     * @return the classLoader
     */
    public Class getClassLoader() {
        if (classLoader == null) {
            classLoader = this.getClass();
        }
        return classLoader;
    }

    /**
     * @param classLoader the classLoader to set
     */
    public void setClassLoader(Class classLoader) {
        this.classLoader = classLoader;
    }

    
}
