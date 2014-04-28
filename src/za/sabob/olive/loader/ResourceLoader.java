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
package za.sabob.olive.loader;

import java.io.*;

/**
 * Provides an interface for loading resources such as SQL files.
 */
public interface ResourceLoader {

    /**
     * Returns the {@link InputStream} of the resource with the given name.
     *
     * @param name the name of the resource for which the InputStream must be returned
     * @return the InputStream for the given source name
     * @throws IllegalStateException if no resource is found for the given source
     */
    public InputStream getResourceStream(String name);
}
