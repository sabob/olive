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

import java.io.InputStream;
import java.util.logging.*;
import javax.servlet.ServletContext;

/**
 * Resource loader that uses the ServletContext of a webapp to
 * load Velocity templates. (it's much easier to use with servlets than
 * the standard FileResourceLoader, in particular the use of war files
 * is transparent).
 *
 * All paths must be absolute to the root of the webapp.
 *
 * To enable caching ensure {@link za.sabob.olive.Olive} is created in {@link za.sabob.olive.Mode#PRODUCTION} mode. To disable the cache
 * and ensure resource changes are reflecting immediately, ensure {@link za.sabob.olive.Olive} is created in
 * {@link za.sabob.olive.Mode#PRODUCTION} mode.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author Nathan Bubna
 * @author <a href="mailto:claude@savoirweb.com">Claude Brisson</a>
 * @version $Id$
 */
public class WebappResourceLoader implements ResourceLoader {

    /** Logger to use for logging messages. */
    private static final Logger LOGGER = Logger.getLogger(WebappResourceLoader.class.getName());

    /**
     * The root paths for templates (relative to webapp's root).
     */
    //protected String[] paths = null;
    //protected HashMap templatePaths = null;
    protected ServletContext servletContext = null;

    public WebappResourceLoader(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * This is abstract in the base class, so we need it.
     * <br>
     * NOTE: this expects that the ServletContext has already
     * been placed in the runtime's application attributes
     * under its full class name (i.e. "javax.servlet.ServletContext").
     *
     * @param configuration the {@link ExtendedProperties} associated with
     * this resource loader.
     */
    /*
     public void init() {
     LOGGER.fine("WebappResourceLoader: initialization starting.");

     // get configured paths
     paths = configuration.getStringArray("path");
     if (paths == null || paths.length == 0) {
     paths = new String[1];
     paths[0] = "/";
     } else {
     // make sure the paths end with a '/' 
     for (int i = 0; i < paths.length; i++) {
     if (!paths[i].endsWith("/")) {
     paths[i] += '/';
     }
     LOGGER.info("WebappResourceLoader: added template path - '" + paths[i] + "'");
     }
     }

     // get the ServletContext 
     Object obj = rsvc.getApplicationAttribute(ServletContext.class.getName());
     if (obj instanceof ServletContext) {
     servletContext = (ServletContext) obj;
     } else {
     LOGGER.severe("WebappResourceLoader: unable to retrieve ServletContext");
     }

     // init the template paths map 
     templatePaths = new HashMap();

     LOGGER.fine("WebappResourceLoader: initialization complete.");
     }*/
    

    /**
     * Get an InputStream so that the Runtime can build a
     * template with it.
     *
     * @param source name of template to get
     * @return InputStream containing the template
     */
    @Override
    public synchronized InputStream getResourceStream(String source) {
        InputStream result = null;

        if (source == null || source.length() == 0) {
            throw new IllegalArgumentException("WebappResourceLoader: source name is required!");
        }

        // since the paths always ends in '/', make sure the name never starts with one
        //while (source.startsWith("/")) {
            //source = source.substring(1);
        //}
        if (!source.startsWith("/")) {
            source = "/" + source;
        }

        Exception exception = null;

        try {
            result = servletContext.getResourceAsStream(source);

            if (result == null) {
                throw new IllegalStateException("WebappResourceLoader: Could not load " + source);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
