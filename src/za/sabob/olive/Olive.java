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

import java.sql.*;
import za.sabob.olive.loader.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

/**
 * Provides the main entry point for Olive. It acts as a facade for the {@link OliveRuntime}.
 * <p/>
 * <h4>Load SQL files</h4>
 * Olive provides the method {@link #loadSql(java.lang.String)} for loading and caching external SQL files. For example:
 * <pre class="prettyprint">
 * Olive olive = new Olive(Mode.DEVELOPMENT);
 * String filename = OliveUtils.normalize(PersonDao.class, "select-person.sql");
 * String sql = olive.loadSql(filename); </pre>
 *
 * <h4>Mode</h4>
 * By default Olive is in {@link za.sabob.olive.Mode#PRODUCTION} mode and loaded SQL files are cached for fast retrieval in the future.
 * <p/>
 * If Olive is created in {@link za.sabob.olive.Mode#DEVELOPMENT} mode, the loaded sql files are not cached and will be reloaded when changed.
 * <p/>
 * {@link za.sabob.olive.Mode#TRACE} mode provides fine grained logging which is useful when tracing errors such as missing named parameters.
 *
 * <pre class="prettyprint">
 * // By default Olive is in PRODUCTION mode.
 * Olive olive = new Olive();
 *
 * // Start Olive in DEVELOPMENT mode
 * olive = new Olive(Mode.DEVELOPMENT);
 *
 * // Start olive in TRACE mode
 * olive = new Olive(Mode.TRACE); </pre>
 *
 * <h4>Named parameters</h4>
 * Use {@link #loadParsedSql(java.lang.String)} to load SQL files and then parse the result to locate named parameters. This method returns
 * a {@link ParsedSql ParsedSql} instance which contains information about the named parameters such as their names and location.
 *
 * <pre class="prettyprint">
 * Olive olive = new Olive();
 * String filename = OliveUtils.normalize(PersonDao.class, "select-person.sql");
 * ParsedSql parsedSql = olive.loadParsedSql(filename);
 * List&lt;int[]&gt; namedParameterIndexes = parsedSql.getParameterIndexes();
 * int totalParameterCount = parsedSql.getTotalParameterCount();
 * String originalSql = parsedSql.getOriginalSql(); </pre>
 *
 * In production mode the ParsedSql is cached so future requests do not have to reparse the SQL string.
 *
 * <h4>PreparedStatements</h4>
 * Olive provides the {@link #prepareStatement(java.sql.Connection, za.sabob.olive.ps.ParsedSql, za.sabob.olive.ps.SqlParams)} and
 * {@link #prepareStatement(java.sql.Connection, java.lang.String, za.sabob.olive.ps.SqlParams)} methods for creating PreparedStatements
 * which uses convenient named parameters instead of indexes.
 * <p/>
 * <code>select-person.sql:</code>
 * <pre class="prettyprint">
 * SELECT * FROM person p WHERE p.name = :name and p.age = :age </pre>
 * <p/>
 * <code>Example.java:</code>
 * <pre class="prettyprint">
 * Connection conn = DriverManager.getConnection("jdbc:...", "username", "password");
 * Olive olive = new Olive(Mode.DEVELOPMENT);
 * String filename = OliveUtils.normalize(Persondao.class, "select-person.sql");
 * SqlParams params = new SqlParams();
 * * params.setString("name", "Bob");
 * params.setInt("age", 21);
 * PreparedStatement ps = olive.prepareStatement(conn, filename, params); </pre>
 *
 */
public class Olive {

    /**
     * Specifies the mode in which Olive is running. The default mode is {@link Mode#PRODUCTION}.
     */
    private static Mode mode = Mode.PRODUCTION;

    /**
     * Specifies the OliveRuntime.
     */
    private OliveRuntime runtime;

    /**
     * Creates a new Olive instance in {@link Mode#PRODUCTION} mode.
     */
    public Olive() {
    }

    /**
     * Create a new Olive instance for the given mode.
     *
     * @param mode the mode for this olive instance
     */
    public Olive(Mode mode) {
        this(mode, (ResourceLoader) null);
    }

    /**
     * Create a new Olive instance for the given ResourceLoader. The resource loaded specifies where SQL files will be loaded from.
     *
     * @param resourceLoader the ResourceLoader for this olive instance
     */
    public Olive(ResourceLoader resourceLoader) {
        runtime = new OliveRuntime(mode, resourceLoader);
    }

    /**
     * Create a new Olive instance for the given ResourceLoader and Mode.
     *
     * @param mode the mode for this olive instance
     * @param resourceLoader the ResourceLoader for this olive instance
     */
    public Olive(Mode mode, ResourceLoader resourceLoader) {
        Olive.mode = mode;
        runtime = new OliveRuntime(mode, resourceLoader);
    }

    /**
     * Create a new Olive instance for the given OliveRuntime.
     *
     * @param runtime the runtime underlying this olive instance
     */
    public Olive(OliveRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Create a new Olive instance for the given OliveRuntime and mode.
     *
     * @param mode the mode for this olive instance
     * @param runtime the runtime underlying this olive instance
     */
    public Olive(Mode mode, OliveRuntime runtime) {
        Olive.mode = mode;
        this.runtime = runtime;
    }

    /**
     * Returns this olive instance resource loader where SQL files will be loaded from.
     *
     * @return this olive instance resource loader.
     */
    public ResourceLoader getResourceLoader() {
        return getRuntime().getResourceLoader();
    }

    /**
     * Sets this olive instance resource loader where SQL files will be loaded from.
     *
     * @param resourceLoader the resource loaded where SQL files will be loaded from
     */
    public void setResourceLoader(ResourceLoader resourceLoader) {
        getRuntime().setResourceLoader(resourceLoader);
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
     * Loads the SQL file for the given filename.
     *
     * <pre class="prettyprint">
     * Olive olive = new Olive();
     * String filename = OliveUtils.normalize(PersonDao.class, "select-person.sql");
     * String sql = olive.loadSql(filename); </pre>
     *
     * @param filename the name of the SQL file to load
     * @return the content of the SQL filename as a string
     * @throws IllegalStateException if the SQL file could not be found
     */
    public String loadSql(String filename) {
        return getRuntime().loadSql(filename);
    }

    /**
     * Loads and parse the SQL file for the given filename.
     *
     * <pre class="prettyprint">
     * Olive olive = new Olive();
     * String filename = OliveUtils.normalize(PersonDao.class, "select-person.sql");
     * ParsedSql parsedSql = olive.loadParsedSql(filename);
     * List&lt;int[]&gt; namedParameterIndexes = parsedSql.getParameterIndexes();
     * int totalParameterCount = parsedSql.getTotalParameterCount();
     * String originalSql = parsedSql.getOriginalSql(); </pre>
     *
     * @param filename the name of the SQL file to load and parse
     * @return the content of the SQL filename as a {@link ParsedSql} instance
     */
    public ParsedSql loadParsedSql(String filename) {
        return getRuntime().loadParsedSql(filename);
    }

    /**
     * Creates a PreparedStatement for the given arguments. The named parameters specified in the {@link ParsedSql} will be
     * substituted with given {@link SqlParams}. For example:
     *
     * <code>select-person.sql:</code>
     * <pre class="prettyprint">
     * SELECT * FROM person p WHERE p.name = :name and p.age = :age </pre>
     * <p/>
     * <code>Example.java:</code>
     * <pre class="prettyprint">
     * Olive olive = new Olive();
     * Connection conn = DriverManager.getConnection("jdbc:...", "username", "password");
     * Olive olive = new Olive(Mode.DEVELOPMENT);
     * String filename = OliveUtils.normalize(Persondao.class, "select-person.sql");
     * ParsedSql parsedSql = olive.loadParsedSql(filename);
     * SqlParams params = new SqlParams();
     * * params.setString("name", "Bob");
     * params.setInt("age", 21);
     * PreparedStatement ps = olive.prepareStatement(conn, parsedSql, params); </pre>
     *
     * @param conn the connection for creating the PreparedStatement with
     * @param parsedSql the ParsedSql for creating the PreparedStatement with
     * @param params the params for creating the PreparedStatement with
     * @return the PreparedStatement for the given arguments
     */
    public PreparedStatement prepareStatement(Connection conn, ParsedSql parsedSql, SqlParams params) {
        PreparedStatement ps = OliveUtils.prepareStatement(conn, parsedSql, params);
        return ps;
    }

    /**
     * Creates a PreparedStatement for the given arguments. The named parameters in SQL file specified through the filename will be
     * substituted with given {@link SqlParams}. For example:
     *
     * <code>select-person.sql:</code>
     * <pre class="prettyprint">
     * SELECT * FROM person p WHERE p.name = :name and p.age = :age </pre>
     * <p/>
     * <code>Example.java:</code>
     * <pre class="prettyprint">
     * Olive olive = new Olive();
     * Connection conn = DriverManager.getConnection("jdbc:...", "username", "password");
     * Olive olive = new Olive(Mode.DEVELOPMENT);
     * String filename = OliveUtils.normalize(Persondao.class, "select-person.sql");
     * SqlParams params = new SqlParams();
     * * params.setString("name", "Bob");
     * params.setInt("age", 21);
     * PreparedStatement ps = olive.prepareStatement(conn, filename, params); </pre>
     *
     * @param conn the connection for creating the PreparedStatement with
     * @param filename the name of the SQL file to load and use for creating the PreparedStatement with
     * @param params the params for creating the PreparedStatement with
     * @return the PreparedStatement for the given arguments
     */
    public PreparedStatement prepareStatement(Connection conn, String filename, SqlParams params) {
        ParsedSql parsedSql = getRuntime().loadParsedSql(filename);
        PreparedStatement ps = prepareStatement(conn, parsedSql, params);
        return ps;
    }
}
