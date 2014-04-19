/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.sabob.olive.loader;

import java.io.*;
import za.sabob.olive.util.*;

/**
 *
 */
public class ClasspathResourceLoader implements ResourceLoader {

    public ClasspathResourceLoader() {
    }

    @Override
    public InputStream getResourceStream(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null!");
        }

        if (!source.startsWith("/")) {
            throw new IllegalArgumentException(
                "source must be absolute (start with a '/'). Use OliveUtils.normalize(cls, source) to create absolute names for resources which are relative to classes.");
        }

        InputStream is = OliveUtils.getResourceAsStream(this.getClass(), source);

        if (is == null) {
            //if (source.startsWith("/")) {
            String pkg = getPackage(source);
            throw new IllegalStateException("The absolute source '" + source + "' cannot be found in the package '" + pkg + "'!");
            /*} else {
             String pkg = getPackage(source);
             pkg = cls.getPackage().getName().replace(".", "/") + "/" + pkg;

             throw new IllegalStateException("The relative source '" + source + "' cannot be found in the package '" + pkg + "'!");
             }*/
        }
        return is;
    }

    protected String getPackage(String filename) {
        int index = filename.lastIndexOf("/");
        String pkg = filename;
        if (index >= 0) {
            pkg = filename.substring(0, index);
        }
        return pkg;
    }
}
