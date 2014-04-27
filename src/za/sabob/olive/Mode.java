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

import java.util.*;

/**
 * Provides the different modes to run an {@link Olive} instance with.
 * <p/>
 * In {@link #PRODUCTION} mode SQL files and parsed SQL strings will be cached for fast retrieval.
 * <p/>
 * * In {@link #DEVELOPMENT} mode SQL files will <em>not</em> be cached and can be dynamically updated.
 * <p/>
 * In {@link #TRACE} mode mode SQL files will <em>not</em> be cached and fine grained logging will be enabled to allow tracing and debugging
 * of the runtime, eg if there are missing named parameters.
 */
public enum Mode {

    /**
     * In production mode SQL files are cached.
     */
    PRODUCTION("prod", 0),
    /**
     * In development mode SQL files are <em>not</em> cached.
     */
    DEVELOPMENT("dev", 1),
    /**
     * In trace mode SQL files are <em>not</em> cached and fine grained logging are enabled.
     */
    TRACE("trace", 2);

    /**
     * The name of the mode.
     */
    private String name;

    /**
     * The level of the mode. PRODUCTION mode is lowest level and TRACE mode the highest.
     */
    private int level;

    /**
     * A map to quickly lookup modes from strings.
     */
    private final static Map<String, Mode> lookup = new HashMap<String, Mode>();

    static {
        for (Mode mode : values()) {
            lookup.put(mode.getName(), mode);
        }
    }

    /**
     * Create a new mode for the give name and level.
     *
     * @param name the name of the mode
     * @param level the level of the mode
     */
    Mode(String name, int level) {
        this.name = name;
        this.level = level;
    }

    /**
     * Return the name of the mode.
     *
     * @return the name of the mode
     */
    public String getName() {
        return name;
    }

    /**
     * Return the mode for the given key or null if no mode is found.
     *
     * @param key the string name to find the mode for
     * @return the mode for the given key or null if no mode is found
     */
    public static Mode getMode(String key) {
        if (key == null) {
            return null;
        }

        return lookup.get(key.toLowerCase());
    }

    /**
     * Return true if the mode is PRODUCTION.
     *
     * @return true if the mode is PRODUCTION
     */
    public boolean isProductionModes() {
        return this.level <= PRODUCTION.level;
    }

    /**
     * Return true if the mode is DEVELOPMENT or TRACE mode.
     *
     * @return true if the mode is DEVELOPMENT or TRACE mode.
     */
    public boolean isDevelopmentModes() {
        return this.level >= DEVELOPMENT.level;
    }

    /**
     * Return the level for the mode.
     *
     * @return the level of the mode
     */
    public int getLevel() {
        return level;
    }
}
