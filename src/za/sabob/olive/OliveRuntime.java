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

import za.sabob.olive.loader.ResourceLoader;
import za.sabob.olive.loader.ClasspathResourceLoader;
import za.sabob.olive.ps.ParsedSql;
import za.sabob.olive.ps.NamedParameterUtils;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import za.sabob.olive.util.*;

/**
 *
 */
public class OliveRuntime {

    private static Map<String, String> fileMap = new ConcurrentHashMap<String, String>();

    private static Map<String, ParsedSql> parsedMap = new ConcurrentHashMap<String, ParsedSql>();

    private Mode mode = Mode.PRODUCTION;

    private ResourceLoader resourceLoader;

    public OliveRuntime() {
        this(Mode.PRODUCTION);
    }

    public OliveRuntime(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public OliveRuntime(Mode mode) {
        this(mode, null);
    }

    public OliveRuntime(Mode mode, ResourceLoader resourceLoader) {
        this.mode = mode;
        this.resourceLoader = resourceLoader;
    }

    public void clearCache() {
        fileMap.clear();
        parsedMap.clear();
    }

    public ParsedSql loadParsedSql(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null!");
        }

        if (getMode() == Mode.PRODUCTION) {
            ParsedSql parsedSql = parsedMap.get(filename);

            if (parsedSql != null) {
                System.out.println("returning cached parsed value");
                return parsedSql;
            }
        }

        String sql = loadFile(filename);
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);

        if (getMode() == Mode.PRODUCTION) {
            parsedMap.put(filename, parsedSql);
        }

        return parsedSql;
    }

    // TODO dtop this method and use solely loadParsedSql?
    public String loadFile(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null!");
        }

        if (getMode() == Mode.PRODUCTION) {
            String file = fileMap.get(filename);
            if (file != null) {
                System.out.println("returning cached value");
                return file;
            }
        }

        InputStream is = getResourceLoader().getResourceStream(filename);

        String file = OliveUtils.toString(is);

        if (getMode() == Mode.PRODUCTION) {
            fileMap.put(filename, file);
        }
        return file;
    }

    /**
     * @return the mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * @return the resourceLoader
     */
    public ResourceLoader getResourceLoader() {
        if (resourceLoader == null) {
            resourceLoader = new ClasspathResourceLoader();
        }
        return resourceLoader;
    }

    /**
     * @param resourceLoader the resourceLoader to set
     */
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
