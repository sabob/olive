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


public enum Mode {

    PRODUCTION("prod", 0),
    DEVELOPMENT("dev", 1),
    TRACE("trace", 2);
    

    private String name;

    private int level;

    private final static Map<String, Mode> lookup = new HashMap<String, Mode>();

    static {
        for (Mode mode : values()) {
            lookup.put(mode.getName(), mode);
        }
    }

    Mode(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public static Mode getMode(String key) {
        if (key == null) {
            return null;
        }

        return lookup.get(key.toLowerCase());
    }

    public boolean isProductionModes() {
        return this.level <= PRODUCTION.level;
    }

    public boolean isDevelopmentModes() {
        return this.level >= DEVELOPMENT.level;
    }

    public int getLevel() {
        return level;
    }
}
