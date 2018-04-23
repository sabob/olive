/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package za.sabob.olive.loader;

import java.io.*;
import java.util.logging.*;
import javax.servlet.*;

/**
 * Resource loader that uses the {@link javax.servlet.ServletContext} of a webapp to load resources (SQL files).
 * <p/>
 * This class is easier to use with Servlets and web environments than {@link ClasspathResourceLoader} as the resources are not specified
 * on the classpath and thus won' cause the server to restart when changes are made to resources.
 * <p/>
 * <b>Note:</b> all paths must be absolute to the root of the webapp.
 * <p>
 * For example, given the following resources in the webapp:
 * 
 * <pre class="prettyprint">
 * /webapp/sql/person/select-person.sql
 * </pre>
 * 
 * Example usage:
 * <pre class="prettyprint">
 * ServletContext servletContext = ...
 * WebappResourceLoader loader = new WebappResourceLoader(servletContext);
 *
 * // We specify the absolute path to the root of the webapp
 * InputStream is = loader.getResourceStream("/sql/person/select-person.sql");
 * String sql = OliveUtils.toString(is);
 * </pre>
 * <p/>
 * To enable caching ensure {@link main.java.za.sabob.olive.Olive} is created in {@link za.sabob.olive.Mode#PRODUCTION} mode.
 * <p/>
 * To disable the cache and ensure resource changes are reflecting immediately, ensure {@link main.java.za.sabob.olive.Olive} is created in
 * {@link za.sabob.olive.Mode#DEVELOPMENT} mode.
 *
 */
public class WebappResourceLoader implements ResourceLoader {

    /**
     * Logger to use for logging messages.
     */
    private static final Logger LOGGER = Logger.getLogger(WebappResourceLoader.class.getName());

    /**
     * The ServletContext to lod resources with.
     */
    protected ServletContext servletContext = null;

    /**
     * Creates a new default WebappResourceLoader for the given {@link javax.servlet.ServletContext}.
     *
     * @param servletContext the servletContext to use for this WebappResourceLoader
     */
    public WebappResourceLoader(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Returns the {@link InputStream} of the resource with the given name.
     * <p/>
     * <b>Note:</b> all paths must be absolute to the root of the webapp.
     * <p/>
     * For example, given the following sql files:
     * 
     * <pre class="prettyprint">
     * /webapp/sql/person/select-person.sql
     * /webapp/sql/product/insert-product.sql
     * </pre>
     * 
     * To load these files you must specify the absolute path to the root of the <em>webapp</em> as follows:
     * 
     * <pre class="prettyprint">
     * ServletContext servletContext = ...
     * WebappResourceLoader loader = new WebappResourceLoader(servletContext);
     * Olive olive = new Olive(loader);
     *
     * // The path is absolute to the root fo the webapp.
     * String personSQL = "/person/select-person.sql";
     * olive.loadSql(personSql);
     * 
     * String productSQL = "/product/insert-product.sql";
     * olive.loadSql(productSql);
     * </pre>
     *
     * @param name the name of the resource for which the InputStream must be returned
     * @return the InputStream for the given source name
     * @throws IllegalStateException if no resource is found for the given source
     */
    @Override
    public InputStream getResourceStream(String name) {
        InputStream result;

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("WebappResourceLoader: source name is required!");
        }

        if (!name.startsWith("/")) {
            name = "/" + name;
        }

        result = servletContext.getResourceAsStream(name);

        if (result == null) {
            throw new IllegalStateException("WebappResourceLoader: Could not load " + name);
        }

        return result;
    }

}
