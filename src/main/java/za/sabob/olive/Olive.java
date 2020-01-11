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

import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.loader.*;
import za.sabob.olive.mustache.*;
import za.sabob.olive.jdbc.ps.*;
import za.sabob.olive.template.*;
import za.sabob.olive.util.OliveUtils;

/**
 * Provides the main entry point for using Olive.
 * <p/>
 * <h4>Load SQL files</h4>
 * Olive provides the method {@link ResourceService#loadContent(java.lang.String)} for loading and caching external SQL files. For example:
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
 * Use {@link ResourceService#loadContent(java.lang.String)} and {@link JDBCUtils#parseSql(String, String)}. This method returns
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
 * Olive provides the {@link #prepareStatement(java.sql.Connection, za.sabob.olive.jdbc.ps.ParsedSql, za.sabob.olive.jdbc.ps.SqlParams)} and
 * JDBCUtils{@link #prepareStatement(Connection, String, SqlParams)} methods for creating PreparedStatements
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

    //private static Map<String, String> fileMap = new ConcurrentHashMap<>();

    //private static Map<String, ParsedSql> parsedMap = new ConcurrentHashMap<>();

    //private static Map<String, Template> templateMap = new ConcurrentHashMap<>();
    private ResourceService resourceService;

    private ResourceLoader resourceLoader;

    /**
     * A templateService for rendering mustache based templates. This provides support for dynamic SQL queries in files.
     */
    private TemplateService templateService;

    private Mustache.Compiler templateCompiler;

    private Mustache.Formatter templateFormatter;

    private Mustache.Collector templateCollector;

    private Mustache.Escaper templateEscaper;

    private Mustache.TemplateLoader templateLoader;

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

    private ResourceService getResourceService() {
        if (this.resourceService == null) {
            this.resourceService = new ResourceService(getResourceLoader());
        }
        return this.resourceService;
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
//    public static Mode getMode() {
//        return mode;
//    }

    /**
     * Set the {@link za.sabob.olive.Mode} for Olive to run in.
     *
     * @param mode the mode Olive must Run in
     */
//    public void setMode( Mode mode ) {
//        Olive.mode = mode;
//    }

    /**
     * Clear Olive's internal cache containing previously loaded SQL files and parsed SQL statements.
     */
//    public void clearCache() {
//        fileMap.clear();
//        parsedMap.clear();
//    }

//    /**
//     * Parse the given SQL statement and find any named parameters contained therein. The parsed SQL is cached under the given name.
//     *
//     * @param name the name under which to cache the parsed sql
//     * @param sqlStr the SQL statement which named parameters is to be parsed
//     * @return a {@link ParsedSql} instance
//     */
//    public static ParsedSql parseSql( String name, String sqlStr ) {
//
//        if ( name == null ) {
//            throw new IllegalArgumentException( "name cannot be null!" );
//        }
//
//        if ( getMode() == Mode.PRODUCTION ) {
//            ParsedSql parsedSql = parsedMap.get( name );
//
//            if ( parsedSql != null ) {
//                return parsedSql;
//            }
//        }
//
//        ParsedSql parsedSql = JDBCUtils.parseSql( sqlStr );
//
//        if ( getMode() == Mode.PRODUCTION ) {
//            parsedMap.put( name, parsedSql );
//        }
//
//        return parsedSql;
//    }
//
//    /**
//     * Loads the content for the given filename.
//     *
//     * <pre class="prettyprint">
//     * Olive olive = new Olive();
//     * String filename = OliveUtils.normalize(PersonDao.class, "data.txt");
//     * String content = olive.loadContent(filename); </pre>
//     *
//     * @param filename the name of the content file to load
//     * @return the content of the filename as a string
//     * @throws IllegalStateException if the file could not be found
//     */
    public String loadContent( String filename ) {
        if ( filename == null ) {
            throw new IllegalArgumentException( "filename cannot be null!" );
        }

        return getResourceService().loadContent( filename );
    }

    /**
     * Loads the SQL file for the given filename. This method delegates to {@link #loadContent(java.lang.String) }.
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
//    public String loadSql( String filename ) {
//        return loadContent( filename );
//    }

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
//    public ParsedSql loadParsedSql( String filename ) {
//        if ( filename == null ) {
//            throw new IllegalArgumentException( "filename cannot be null!" );
//        }
//
//        if ( getMode() == Mode.PRODUCTION ) {
//            ParsedSql parsedSql = parsedMap.get( filename );
//
//            if ( parsedSql != null ) {
//                return parsedSql;
//            }
//        }
//
//        String sql = loadSql( filename );
//        ParsedSql parsedSql = JDBCUtils.parseSql( sql );
//
//        if ( getMode() == Mode.PRODUCTION ) {
//            parsedMap.put( filename, parsedSql );
//        }
//
//        return parsedSql;
//    }

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
        PreparedStatement ps = JDBCUtils.prepareStatement( conn, parsedSql, params );
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
        PreparedStatement ps = JDBCUtils.prepareStatement( conn, parsedSql, params, autoGeneratedKeys );
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
        PreparedStatement ps = JDBCUtils.prepareStatement( conn, parsedSql, params, resultSetType, resultSetConcurrency, resultSetHoldability );
        return ps;
    }

//    public PreparedStatement prepareStatementFromFile( JDBCContext ctx, String filename, SqlParams params ) {
//
//        Connection conn = ctx.getConnection();
//
//        return prepareStatementFromFile( conn, filename, params  );
//    }

//    /**
//     * Creates a PreparedStatement for the given filename. The named parameters in SQL file specified through the filename will be
//     * substituted with given {@link SqlParams}. For example:
//     *
//     * <code>select-person.sql:</code>
//     * <pre class="prettyprint">
//     * SELECT * FROM person p WHERE p.name = :name and p.age = :age </pre>
//     * <p/>
//     * <code>Example.java:</code>
//     * <pre class="prettyprint">
//     * Olive olive = new Olive();
//     * Connection conn = DriverManager.getConnection("jdbc:...", "username", "password");
//     * Olive olive = new Olive(Mode.DEVELOPMENT);
//     * String filename = OliveUtils.normalize(Persondao.class, "select-person.sql");
//     * SqlParams params = new SqlParams();
//     * * params.setString("name", "Bob");
//     * params.setInt("age", 21);
//     * PreparedStatement ps = olive.prepareStatement(conn, filename, params); </pre>
//     *
//     * @param conn the connection for creating the PreparedStatement with
//     * @param filename the name of the SQL file to load and use for creating the PreparedStatement with
//     * @param params the params for creating the PreparedStatement with
//     * @return the PreparedStatement for the given arguments
//     */
//    public PreparedStatement prepareStatementFromFile( Connection conn, String filename, SqlParams params ) {
//        ParsedSql parsedSql = loadParsedSql( filename );
//        PreparedStatement ps = prepareStatement( conn, parsedSql, params );
//        return ps;
//    }

    public PreparedStatement prepareStatement( Connection conn, String content, SqlParams params ) {
        ParsedSql parsedSql = JDBCUtils.parseSql( content );
        PreparedStatement ps = prepareStatement( conn, parsedSql, params );
        return ps;
    }

//    public PreparedStatement prepareStatementFromTemplateFile( Connection conn, String filename, SqlParams params ) {
//        // TODO load map from SqlParams
//        Map data = params.toMap();
//        String result = executeTemplateFile( filename, data );
//        ParsedSql parsedSql = JDBCUtils.parseSql( result );
//        PreparedStatement ps = prepareStatement( conn, parsedSql, params );
//        return ps;
//    }
//
//    public PreparedStatement prepareStatementFromTemplate( Connection conn, String content, SqlParams params, Map data ) {
//        String result = executeTemplate( content, data );
//        ParsedSql parsedSql = JDBCUtils.parseSql( result );
//        PreparedStatement ps = prepareStatement( conn, parsedSql, params );
//        return ps;
//    }
//
//    public PreparedStatement prepareStatementFromTemplateFile( Connection conn, String filename, SqlParams params, Map data ) {
//
//        String result = executeTemplateFile( filename, data );
//        ParsedSql parsedSql = JDBCUtils.parseSql( result );
//        PreparedStatement ps = prepareStatement( conn, parsedSql, params );
//        return ps;
//    }

//    public String executeTemplateFile( String filename, Map data ) {
//        Template template = loadCompiledTemplate( filename, data );
//        String result = executeTemplate( template, data );
//        return result;
//    }

//    public String executeTemplateFromFile( String filename, Map data ) {
//        String result = executeTemplateFile( filename, data );
//        return result;
//    }
//
//    public String executeTemplate( String name, String content, Map data ) {
//        Template template = compileTemplate( content );
//        String result = executeTemplate( template, data );
//        return result;
//    }

    public String executeTemplate( String content, Map data ) {
        Template template = getTemplateService().compile( content, data );

        String result = executeTemplate( template, data );
        return result;
    }

    public String executeTemplate( Template template, Map data ) {
        String result = getTemplateService().execute( template, data );
        return result;
    }

//    public Template loadCompiledTemplate( String filename ) {
//
//        Map data = new HashMap();
//        return loadCompiledTemplate( filename, data );
//    }

//    public Template loadCompiledTemplate( String filename, Map data ) {
//
//        String content = loadContent( filename );
//        Template template = getTemplateService().compileTemplate( content, data );
//        return template;
//    }

//    public Template compileTemplate( String name, String content ) {
//
//        Map data = new HashMap();
//        Template template = compileTemplate( name, content, data );
//        return template;
//    }

//    public Template compileTemplate( String name, String content, Map data ) {
//        // Bercause of partials, DONT cache
////        if ( getMode() == Mode.PRODUCTION ) {
////            Template template = templateMap.get( name );
////
////            if ( template != null ) {
////                return template;
////            }
////        }
//
//        Template template = getTemplateService().compileTemplate( content, data );
//
////        if ( getMode() == Mode.PRODUCTION ) {
////            templateMap.put( name, template );
////        }
//        return template;
//    }

    public Template compileTemplate( String content ) {
        Map data = new HashMap();
        Template template = getTemplateService().compile( content, data );
        return template;
    }

    public TemplateService getTemplateService() {
        if ( templateService == null ) {
            templateService = new TemplateService();
        }

        return templateService;
    }

    public void setTemplateService( TemplateService templateService ) {
        this.templateService = templateService;
    }

    public static void main( String[] args ) {

        long start = System.currentTimeMillis();
        int loop = 1;

        String result = "";
        Olive olive = new Olive( Mode.PRODUCTION );

        for ( int i = 0; i < loop; i++ ) {

            String text = "One, two, {{three}}. Three sir!";
            //Template tmpl = Mustache.compiler().compile( text );
            Map<String, String> data = new HashMap<>();
            data.put( "three", "five" );
            //System.out.println( tmpl.execute( data ) );

            result = olive.executeTemplate( text, data );

        }

        long end = System.currentTimeMillis();

        System.out.println( result + ", in " + (end - start) );
        // result: "One, two, five. Three sir!"

    }
}
