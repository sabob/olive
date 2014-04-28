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
import za.sabob.olive.util.*;

/**
 * Provides the default {@link ResourceLoader} implementation for Olive.
 * <p/>
 * The ClasspathResourceLoader will load resources (SQL files) from the classpath.
 * <p/>
 * <b>Note:</b> all paths must be absolute to the root of the classpath.
 * <p/>
 * Example usage:
 * <pre class="prettyprint">
 * /com/mycorp/dao/person/select-person.sql
 * /com/mycorp/dao/person/PersonDao.class
 * </pre>
 * <pre class="prettyprint">
 * // ClasspathResourceLoader is the default loader
 * ClasspathResourceLoader loader = new ClasspathResourceLoader();
 * 
 * // Specify the absolute path to load the file
 * InputStream is = loader.getResourceStream("/com/mycorp/dao/person/select-person.sql");
 * String sql = OliveUtils.toString(is);
 * 
 * // Alternatively use OliveUtils.normalize to create the absolute path relative to the PersonDao.class
 * filename = OliveUtils.normalize(PersonDao.class, "select-person.sql");
 * is = loader.getResourceStream("/com/mycorp/dao/person/select-person.sql");
 * sql = OliveUtils.toString(is);
 * </pre>
 * 
 * <b>Note:</b>ClasspathClassLoader is the default {@link ResourceLoader} used for Olive. Thus there is no need to specify ClasspathClassLoader
 * when creating an Olive instance, for example:
 * 
 *  * <pre class="prettyprint">
 * // ClasspathResourceLoader is the default loader
 * Olive olive = new Olive();
 * String filename = OliveUtils.normalize(PersonDao.class, "select-person.sql");
 * String sql = olive.loadSql(filename); 
 * </pre>
 * 
 * <b>Advantage:</b> ClasspathResourceLoader is easy to setup and use. Simply pass the absolute filename to {@link #getResourceStream(java.lang.String)}
 * and it will return the content of the filename.
 * <p/>
 * <b>Disadvantage:</b> Since the resources (SQL files) are placed on the classpath, changes made to the resources <em>might</em> cause the runtime
 * to restart. For example in Tomcat, changes to the resources on the classpath will cause Tomcat to restart. Therefore it is recommended to
 * use {@link WebappResourceLoader} in Servlet environments such as Tomcat.
 * 
 */
public class ClasspathResourceLoader implements ResourceLoader {

    /**
     * Creates a new default ClasspathResourceLoader instance.
     */
    public ClasspathResourceLoader() {
    }

    /**
     * Returns the {@link InputStream} of the resource with the given name.
     * 
     *  * The name must be the absolute path to the resource in the classpath. The name <em>must</em> start with a <strong>'/'</strong>.
     * <p/>
     * For example, given the following sql files and classes on the classpath:
     * 
     * <pre class="prettyprint">
     * /com/mycorp/dao/person/PersonDao.class
     * /com/mycorp/dao/person/select-person.sql
     * 
     * /com/mycorp/dao/product/ProductDao.class
     * /com/mycorp/dao/product/insert-product.sql
     * </pre>
     * 
     * To load these files you must specify the absolute path to the files in the classpath as follows:
     * 
     * <pre class="prettyprint">
     * // ClasspathResourceLoader is the default loaded, so no need to specify it
     * Olive olive = new Olive();
     * 
     * // We specify the absolute name under the webapp folder, but not including the "webapp" folder name.
     * String personSQL = "/com/mycorp/dao/person/select-person.sql";
     * olive.loadSql(personSql);
     * 
     * // Alternatively we could also use OliveUtils to normalize the file dpath relative to a class
     * String productSQL = OliveUtils.normalize(ProductDao.class, "insert-product.sql");
     * olive.loadSql(productSql);
     * </pre>
     * 
     * @param name the name of the resource for which the InputStream must be returned
     * @return the InputStream for the given source
     * @throws IllegalStateException if no resource is found for the given source
     */
    @Override
    public InputStream getResourceStream(String name) {
        if (name == null) {
            throw new IllegalArgumentException("source cannot be null!");
        }

        if (!name.startsWith("/")) {
            throw new IllegalArgumentException(
                "source must be absolute (start with a '/'). Use OliveUtils.normalize(cls, source) to create absolute names for resources which are relative to classes.");
        }

        InputStream is = OliveUtils.getResourceAsStream(this.getClass(), name);

        if (is == null) {
            String folder = getFolder(name);
            throw new IllegalStateException("The absolute source '" + name + "' cannot be found in the package '" + folder + "'!");
        }
        return is;
    }

    /**
     * Return the folder of the filename.
     *
     * @param filename the filename which folder must be returned
     * @return the folder of the filename
     */
    private String getFolder(String filename) {
        int index = filename.lastIndexOf("/");
        String pkg = filename;
        if (index >= 0) {
            pkg = filename.substring(0, index);
        }
        return pkg;
    }
}
