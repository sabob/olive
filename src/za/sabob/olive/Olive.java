/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.sabob.olive;

import za.sabob.olive.loader.*;
import za.sabob.olive.ps.ParsedSql;

/**
 *
 */
public class Olive {

    private static Mode mode = Mode.PRODUCTION;

    private OliveRuntime runtime;

    public Olive() {
    }

    public Olive(Mode mode) {
        this(mode, null);
    }

    public Olive(Class classLoader) {
        this(Mode.PRODUCTION, classLoader);
    }

    public Olive(Mode mode, Class classLoader) {
        this.mode = mode;
        ClasspathResourceLoader loader = new ClasspathResourceLoader(classLoader);
        runtime = new OliveRuntime(mode, loader);
    }

    public Olive(ResourceLoader resourceLoader) {
        runtime = new OliveRuntime(mode, resourceLoader);
    }

    public ResourceLoader getResourceLoader() {
        return getRuntime().getResourceLoader();
    }

    public void setResourceLoader(ResourceLoader loader) {
        getRuntime().setResourceLoader(loader);
    }

    /**
     * @return the runtime
     */
    public OliveRuntime getRuntime() {
        if (runtime == null) {
            runtime = new OliveRuntime(getMode());
        }
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setRuntime(OliveRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * @return the mode
     */
    public static Mode getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(Mode mode) {
        Olive.mode = mode;
        getRuntime().setMode(mode);
    }

    /**
     * @return the mode
     */
    public Class getClassLoader() {
        ClasspathResourceLoader classpathLoader = getClasspathResourceLoader();
        if (classpathLoader != null) {
            return classpathLoader.getClassLoader();
        }
        return null;
    }

    /**
     *
     */
    public void setClassLoader(Class classLoader) {
        ClasspathResourceLoader classpathLoader = getClasspathResourceLoader();
        if (classpathLoader != null) {
            classpathLoader.setClassLoader(classLoader);
        }
    }

    private ClasspathResourceLoader getClasspathResourceLoader() {
        ResourceLoader loader = getRuntime().getResourceLoader();
        if (loader instanceof ClasspathResourceLoader) {
            ClasspathResourceLoader classPathLoader = (ClasspathResourceLoader) loader;
            return classPathLoader;
        }
        return null;
    }
    
    public ParsedSql loadParsedSql(String filename) {
        return getRuntime().loadParsedSql(filename);
    }
        
    public ParsedSql loadParsedSql(String filename, Class classLoader) {
        ClasspathResourceLoader classpathLoader = getClasspathResourceLoader();
        
        Class origClassLoader = null;
        if (classpathLoader != null) {
            origClassLoader = classpathLoader.getClassLoader();
            classpathLoader.setClassLoader(classLoader);
        }

        ParsedSql parsedSql = getRuntime().loadParsedSql(filename);
        
        // Restore original class loader
        if (classLoader != null) {
            classpathLoader.setClassLoader(origClassLoader);
        }
        
        return parsedSql;
    }

    public String loadFile(String filename) {
        return getRuntime().loadFile(filename);
    }

    public String loadFile(String filename, Class classLoader) {
        ClasspathResourceLoader classpathLoader = getClasspathResourceLoader();
        
        Class origClassLoader = null;
        if (classpathLoader != null) {
            origClassLoader = classpathLoader.getClassLoader();
            classpathLoader.setClassLoader(classLoader);
        }

        String file = getRuntime().loadFile(filename);
        
        // Restore original class loader
        if (classLoader != null) {
            classpathLoader.setClassLoader(origClassLoader);
        }
        
        return file;
    }
}
