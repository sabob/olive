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

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import za.sabob.olive.loader.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.transaction.*;
import za.sabob.olive.util.*;

/**
 * Provides the main entry point for using Olive.
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
     * Logger instance for logging messages.
     */
    private static final Logger LOGGER = Logger.getLogger( Olive.class.getName() );

    private static Map<String, String> fileMap = new ConcurrentHashMap<String, String>();

    private static Map<String, ParsedSql> parsedMap = new ConcurrentHashMap<String, ParsedSql>();

    private ResourceLoader resourceLoader;

    /**
     * Specifies the mode in which Olive is running. The default mode is {@link Mode#PRODUCTION}.
     */
    private static Mode mode = Mode.PRODUCTION;

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
    public Olive( Mode mode ) {
        this( mode, (ResourceLoader) null );
    }

    /**
     * Create a new Olive instance for the given ResourceLoader. The resource loaded specifies where SQL files will be loaded from.
     *
     * @param resourceLoader the ResourceLoader for this olive instance
     */
    public Olive( ResourceLoader resourceLoader ) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Create a new Olive instance for the given ResourceLoader and Mode.
     *
     * @param mode the mode for this olive instance
     * @param resourceLoader the ResourceLoader for this olive instance
     */
    public Olive( Mode mode, ResourceLoader resourceLoader ) {
        Olive.mode = mode;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns this olive instance resource loader where SQL files will be loaded from.
     *
     * @return this olive instance resource loader.
     */
    public ResourceLoader getResourceLoader() {
        if ( resourceLoader == null ) {
            resourceLoader = new ClasspathResourceLoader();
        }
        return resourceLoader;
    }

    /**
     * Sets this olive instance resource loader where SQL files will be loaded from.
     *
     * @param resourceLoader the resource loaded where SQL files will be loaded from
     */
    public void setResourceLoader( ResourceLoader resourceLoader ) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns the {@link za.sabob.olive.Mode} Olive is running in.
     *
     * @return the mode Olive is running in
     */
    public static Mode getMode() {
        return mode;
    }

    /**
     * Set the {@link za.sabob.olive.Mode} for Olive to run in.
     *
     * @param mode the mode Olive must Run in
     */
    public void setMode( Mode mode ) {
        Olive.mode = mode;
    }

    /**
     * Clear Olive's internal cache containing previously loaded SQL files and parsed SQL statements.
     */
    public void clearCache() {
        fileMap.clear();
        parsedMap.clear();
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
    public String loadSql( String filename ) {
        if ( filename == null ) {
            throw new IllegalArgumentException( "filename cannot be null!" );
        }

        if ( getMode() == Mode.PRODUCTION ) {
            String file = fileMap.get( filename );
            if ( file != null ) {
                if ( getMode() == Mode.TRACE ) {
                    LOGGER.info( "return the cached sql for filename '" + filename + "'" );
                }
                return file;
            }
        }

        InputStream is = getResourceLoader().getResourceStream( filename );

        String file = OliveUtils.toString( is );

        if ( getMode() == Mode.PRODUCTION ) {
            fileMap.put( filename, file );
        }
        return file;
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
    public ParsedSql loadParsedSql( String filename ) {
        if ( filename == null ) {
            throw new IllegalArgumentException( "filename cannot be null!" );
        }

        if ( getMode() == Mode.PRODUCTION ) {
            ParsedSql parsedSql = parsedMap.get( filename );

            if ( parsedSql != null ) {
                if ( getMode() == Mode.TRACE ) {
                    LOGGER.info( "return the cached ParsedSql for filename '" + filename + "'" );
                }
                return parsedSql;
            }
        }

        String sql = loadSql( filename );
        ParsedSql parsedSql = OliveUtils.parseSql( sql );

        if ( getMode() == Mode.PRODUCTION ) {
            parsedMap.put( filename, parsedSql );
        }

        return parsedSql;
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
    public PreparedStatement prepareStatement( Connection conn, ParsedSql parsedSql, SqlParams params ) {
        PreparedStatement ps = OliveUtils.prepareStatement( conn, parsedSql, params );
        return ps;
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
     * @param autoGeneratedKeys specifies the autoGenerated keys value of: Statement.RETURN_GENERATED_KEYS or Statement.NO_GENERATED_KEYS
     *
     * @return the PreparedStatement for the given arguments
     */
    public PreparedStatement prepareStatement( Connection conn, ParsedSql parsedSql, SqlParams params, int autoGeneratedKeys ) {
        PreparedStatement ps = OliveUtils.prepareStatement( conn, parsedSql, params, autoGeneratedKeys );
        return ps;
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
     * @param resultSetType - one of the following ResultSet constants: ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency - one of the following ResultSet constants: ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability - one of the following ResultSet constants: ResultSet.HOLD_CURSORS_OVER_COMMIT or ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @return the PreparedStatement for the given arguments
     */
    public PreparedStatement prepareStatement( Connection conn, ParsedSql parsedSql, SqlParams params, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability ) {
        PreparedStatement ps = OliveUtils.prepareStatement( conn, parsedSql, params, resultSetType, resultSetConcurrency, resultSetHoldability );
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
    public PreparedStatement prepareStatement( Connection conn, String filename, SqlParams params ) {
        ParsedSql parsedSql = loadParsedSql( filename );
        PreparedStatement ps = prepareStatement( conn, parsedSql, params );
        return ps;
    }

    public static Connection BEGIN( Connection conn ) {
        return OliveTransaction.beginTransaction( conn );
    }

    public static void COMMIT( Connection conn ) {
        OliveTransaction.commitTransaction( conn );
    }

    public static void ROLLBACK_AND_THROW( Connection conn, Exception ex ) {
        OliveTransaction.rollbackTransaction( conn, ex );
    }

    public static void CLOSE( AutoCloseable... closeables ) {
        OliveTransaction.closeTransaction( closeables );
    }
}
