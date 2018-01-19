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
package za.sabob.olive.util;

import java.io.*;
import za.sabob.olive.ps.SqlParam;
import za.sabob.olive.ps.ParsedSql;
import za.sabob.olive.ps.SqlParams;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Provides common utilities:
 * <ul>
 * <li>easily closing {@link #close(java.sql.Connection) Connections}, {@link #close(java.sql.ResultSet) ResultSets},
 * and {@link #close(java.sql.Statement) Statements}.</li>
 * <li>{@link #normalize(java.lang.Class, java.lang.String)} returns the absolute path for a resource that is relative to a given class.</li>
 * <li>Create {@link #prepareStatement(java.sql.Connection, za.sabob.olive.ps.ParsedSql, za.sabob.olive.ps.SqlParams) preparedStatements}
 * for named parameters.</li>
 * </ul>
 *
 * <pre class="prettyprint">
 * Connection conn = ...
 * Olive olive =new Olive();
 * SqlParams params = new SqlParams();
 * params.setString("name", "Steve");
 *
 * String sql = olive.loadSql(fullname);
 * String fullname = OliveUtils.normalize(PersonDao.class, "insert_person.sql");
 * ParseSql parseSql = OliveUtils.parseSql(sql);
 * PreparedStatement ps = OliveUtils.prepareStatement(Connection conn, ParsedSql parsedSql, SqlParams params);
 * ...
 * </pre>
 *
 */
public class OliveUtils {

    /**
     * Not found indicator.
     */
    private static final int NOT_FOUND = -1;

    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * The system separator character.
     */
    private static final char CLASSPATH_SEPARATOR = UNIX_SEPARATOR;

    /**
     * Represents the end-of-file (or stream).
     */
    public static final int EOF = -1;

    /**
     * The default buffer size ({@value}) to use for
     * {@link #copyLarge(InputStream, OutputStream)}
     * and
     * {@link #copyLarge(Reader, Writer)}
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Logger instance for logging messages.
     */
    private static final Logger LOGGER = Logger.getLogger( OliveUtils.class.getName() );

    /**
     * Indicates and unknown SQL type.
     */
    public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;

    /**
     * Commit the given connection and wraps SQLExceptions as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * <pre class="prettyprint">
     * Connection conn;
     * PreparedStatement ps;
     *
     * try {
     * conn = ...;
     *
     *     // We'll handle our own transactions so switch off autocommit
     * conn.setAutoCommit(false);
     *
     * Olive olive = new Olive();
     * ParsedSql sql = olive.loadParsedSql("/org/mycorp/dao/person/insert_person.sql");
     * SqlParams params = new SqlParams();
     * params.setString("name", "Steve Sanders");
     * params.setInt("age", 21);
     * ps = olive.prepareStatement(conn, sql, params);
     * ps.executeUpdate();
     *
     *     // Commit the transaction
     * OliveUtils.commit(conn);
     *
     * } catch (Exception e) {
     *     // Rollback the transaction
     * OliveUtils.rollback(conn, e);
     *
     * } finally {
     *     // Close resources
     * OliveUtils.close(ps, conn);
     * }
     * </pre>
     *
     * @param conn the connection to commit
     */
    public static void commit( Connection conn ) {
        try {
            if ( conn != null ) {
                conn.commit();
            }
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Rollback the given connection and throw the sqlException as a RuntimeException. Any SQLExceptions thrown will be logged by the
     * {@link #LOGGER}.
     * <p/>
     * This method is null safe, so the connection and sqlException can be null.
     *
     * <pre class="prettyprint">
     *
     * Connection conn;
     * PreparedStatement ps;
     *
     * try {
     *
     * conn = ...
     *
     *     // We'll handle our own transactions so switch off autocommit
     * conn.setAutoCommit(false);
     *
     * Olive olive = new Olive();
     * ParsedSql sql = olive.loadParsedSql("/org/mycorp/dao/person/insert_person.sql");
     * SqlParams params = new SqlParams();
     * params.setString("name", "Steve Sanders");
     * params.setInt("age", 21);
     * ps = olive.prepareStatement(conn, sql, params);
     * ps.executeUpdate();
     *
     *     // Commit the transaction
     * OliveUtils.commit(conn);
     *
     *
     * } catch (Exception e) {
     *     // Rollback the transaction
     * OliveUtils.rollback(conn, e);
     * } finally {
     *
     *     // Close resources
     * OliveUtils.close(ps, conn);
     * }
     *
     * </pre>
     *
     * @param conn the connection to rollback
     * @param exception the Exception that is causing the transaction to be rolled backs
     */
    public static void rollback( Connection conn, Exception exception ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            LOGGER.log( Level.SEVERE, e.getMessage(), e );
        }

        if ( exception instanceof RuntimeException ) {

            throw (RuntimeException) exception;
        }

        if ( exception != null ) {
            throw new RuntimeException( exception );
        }
    }

    /**
     * Closes the given resultset and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the resultset can be null.
     *
     * @param rs the resultset to close
     */
    public static void closeResultSet( ResultSet rs ) {
        try {
            if ( rs != null ) {
                rs.close();
            }
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Closes the given statement and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the statement can be null.
     *
     * @param st the statement to close
     */
    public static void closeStatement( Statement st ) {
        try {
            if ( st != null ) {
                st.close();
            }
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Closes the given connection and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param conn the connection to close
     */
    public static void closeConnection( Connection conn ) {
        try {
            if ( conn != null ) {
                conn.close();
            }
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Closes the given statement and connection and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * <b>Note:</b> only the first exception thrown by closing the statement and connection will be thrown as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the statement and connection can be null.
     *
     * @param st the statement to close
     * @param conn the connection to close
     */
    public static void close( Statement st, Connection conn ) {

        // Track the first exception thrown
        RuntimeException origException = null;

        try {
            closeStatement( st );
        } catch ( RuntimeException e ) {

            origException = e;
        }

        try {
            closeConnection( conn );

        } catch ( RuntimeException e ) {
            if ( origException == null ) {
                origException = e;
            }
        }

        if ( origException != null ) {
            throw origException;
        }
    }

    /**
     * Closes the given resultset, statement and connection and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * <b>Note:</b> only the first exception thrown by closing the resultset, statement and connection will be thrown as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the resultset, statement and connection can be null.
     *
     * @param rs the resultset to close
     * @param st the statement to close
     * @param conn the connection to close
     */
    public static void close( ResultSet rs, Statement st, Connection conn ) {

        // Track the first exception thrown
        RuntimeException origException = null;

        try {
            closeResultSet( rs );

        } catch ( RuntimeException e ) {
            origException = e;
        }

        try {
            closeStatement( st );
        } catch ( RuntimeException e ) {

            if ( origException == null ) {
                origException = e;
            }
        }

        try {
            closeConnection( conn );
        } catch ( RuntimeException e ) {
            if ( origException == null ) {
                origException = e;
            }
        }

        if ( origException != null ) {
            throw origException;
        }
    }

    /**
     * Closes the given resultset and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the resultset can be null.
     *
     * @param rs the resultset to close
     */
    public static void close( ResultSet rs ) {
        closeResultSet( rs );
    }

    /**
     * Closes the given statement and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the statement can be null.
     *
     * @param st the statement to close
     */
    public static void close( Statement st ) {
        closeStatement( st );
    }

    /**
     * Closes the given connection and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param conn the connection to close
     */
    public static void close( Connection conn ) {
        closeConnection( conn );
    }

    /**
     * Sets the connection autoCommit mode and and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the connection can be null.
     * 
     * @param conn the connection on which to set autoCommit
     * @param bool true to enable setAutoCommit, false to disable autoCommit
     */
    public static void setAutoCommit( Connection conn, boolean bool ) {

        try {
            if ( conn != null ) {
                conn.setAutoCommit( bool );
            }

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Parse the given SQL statement and find any named parameters contained therein.
     *
     * @param sqlStr the SQL statement which named parameters is to be parsed
     * @return a {@link ParsedSql} instance
     */
    public static ParsedSql parseSql( String sqlStr ) {
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement( sqlStr );
        return parsedSql;
    }

    /**
     * Replace all named parameters with the given parameters.
     * <p/>
     * Named parameters are substituted for a JDBC placeholder ('?'), and any select list is
     * expanded to the required number of placeholders. Select lists may contain an array or Collection of
     * objects, and in that case the placeholders will be grouped and enclosed with
     * parentheses. This allows for the use of "expression lists" in the SQL statement
     * like: <br><br>
     * {@code select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))}
     * <p>
     * The parameter values passed in are used to determine the number of placeholders to
     * be used for a select list. Select lists should be limited to 100 or fewer elements.
     * A larger number of elements is not guaranteed to be supported by the database and
     * is strictly vendor-dependent.
     * @param parsedSql the parsed representation of the SQL statement
     * @param parameters the source for named parameters
     * @return the SQL statement with substituted parameters
     */
    public static String substituteNamedParameters( ParsedSql parsedSql, SqlParams parameters ) {

        String sql = NamedParameterUtils.substituteNamedParameters( parsedSql, parameters );
        return sql;
    }

    /**
     * Create and return a PreparedStatement for the given connection, parsedSql and parameters.
     * <p/>
     * the PreparedStatement will have all it's named parameters replaced by the given parameters
     *
     * @param conn the connection to create the PreparedStatement with
     * @param parsedSql the parsed representation of the SQL statement
     * @param parameters the source for named parameters
     * @return the PreparedStatement with all named parameters replaced by the given parameters
     */
    public static PreparedStatement prepareStatement( Connection conn, ParsedSql parsedSql, SqlParams parameters ) {
        String sql = NamedParameterUtils.substituteNamedParameters( parsedSql, parameters );
        try {
            PreparedStatement ps = conn.prepareStatement( sql );
            setParams( ps, parsedSql, parameters );
            return ps;

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Create and return a PreparedStatement for the given connection, SQL statement and parameters.
     * <p/>
     * The SQL statement will be parsed and all named parameters will be found.
     * <p/>
     * the PreparedStatement will have all it's named parameters replaced by the given parameters
     *
     * @param conn the connection to create the PreparedStatement with
     * @param sqlStatement a SQL statement
     * @param parameters the source for named parameters
     * @return the PreparedStatement with all named parameters replaced by the given parameters
     */
    public static PreparedStatement prepareStatement( Connection conn, String sqlStatement, SqlParams parameters ) {

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement( sqlStatement );
        String sql = NamedParameterUtils.substituteNamedParameters( parsedSql, parameters );

        try {
            PreparedStatement ps = conn.prepareStatement( sql );
            setParams( ps, parsedSql, parameters );
            return ps;

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Replace the named parameters defined on the parsedSql with JDBC placeholders ('?') on the PreparedStatement for the given parameters.
     *
     * @param ps the PreparedStatement which named parameters must be replaced with JDBC placeholders('?')
     * @param parsedSql the parsed sql which named parameters must be replaced
     * @param parameters the source for named parameters
     */
    public static void setParams( PreparedStatement ps, ParsedSql parsedSql, SqlParams parameters ) {

        SqlParam[] input = NamedParameterUtils.buildValueArray( parsedSql, parameters );
        if ( input != null ) {
            for ( int i = 0; i < input.length; i++ ) {
                SqlParam arg = input[i];
                setParam( ps, i + 1, arg );
            }
        }
    }

    /**
     * Replace the named parameter defined at the given index with a JDBC placeholder('?') on the PreparedStatement.
     *
     * @param ps the PreparedStatement which named parameter must be replaced with a JDBC placeholder('?')
     * @param indexPosition the index where the named parameter is defined in the PreparedStatement
     * @param parameter the parameter value that must replace the named parameter
     */
    public static void setParam( PreparedStatement ps, int indexPosition, SqlParam parameter ) {

        try {
            StatementUtils.setParameterValue( ps, indexPosition, parameter );

            /*
             if (param.hasSqlType()) {

             //if (param.getSqlType() == Types.VARCHAR) {
             //  ps.setString(indexPosition, (String) param.getValue());
             //} else {
             if (param.getScale() == null) {
             ps.setObject(indexPosition, param.getValue(), param.getSqlType());

             } else {
             ps.setObject(indexPosition, param.getValue(), param.getSqlType(), param.getScale());
             }

             //}
             } else {
             ps.setObject(indexPosition, param.getValue());
             }
             */
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Truncate the string at the given maximum width. If the string length does not exceed the maximum width, the original string is
     * returned, otherwise the string is truncated to the maximum width.
     *
     * @param str the string to truncate
     * @param maxWidth the maximum width of the string
     * @return the truncated string
     */
    public static String truncate( String str, int maxWidth ) {
        if ( isBlank( str ) ) {
            return str;
        }

        if ( str.length() <= maxWidth ) {
            return str;
        }

        return str.substring( 0, maxWidth );
    }

    /**
     * Return true if a CharSequence is whitespace, empty ("") or null.
     *
     * @param cs the CharSequence to check, may be null
     * @return true if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank( final CharSequence cs ) {
        int strLen;
        if ( cs == null || (strLen = cs.length()) == 0 ) {
            return true;
        }
        for ( int i = 0; i < strLen; i++ ) {
            if ( Character.isWhitespace( cs.charAt( i ) ) == false ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return false if a CharSequence is whitespace, empty ("") or null.
     *
     * @param cs the CharSequence to check, may be null
     * @return false if the CharSequence is null, empty or whitespace
     */
    public static boolean isNotBlank( final CharSequence cs ) {
        return !isBlank( cs );
    }

    /**
     * Returns the contents of an InputStream as a String using "UTF-8" encodings.
     *
     * @param input the InputStream to read from
     * @return the contents of the InputStream as a String
     */
    public static String toString( final InputStream input ) {
        final StringBuilderWriter sw = new StringBuilderWriter();
        copy( input, sw, "utf-8" );
        return sw.toString();
    }

    /**
     * Finds a resource with the given name. Checks the Thread Context classloader, then uses the System classloader.
     * <p/>
     * Should replace all calls to <code>Class.getResourceAsString</code> when the resource might come from a different classloader.
     * (e.g. a webapp).
     *
     * @param cls Class to use when getting the System classloader (used if no Thread Context classloader available or fails to get resource).
     * @param name name of the resource
     * @return InputStream for the resource.
     */
    public static InputStream getResourceAsStream( Class cls, String name ) {
        InputStream result = null;

        /**
         * remove leading slash so path will work with classes in a JAR file
         */
        while ( name.startsWith( "/" ) ) {
            name = name.substring( 1 );
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if ( classLoader == null ) {
            classLoader = cls.getClassLoader();
            result = classLoader.getResourceAsStream( name );
        } else {
            result = classLoader.getResourceAsStream( name );

            /**
             * for compatibility with texen / ant tasks, fall back to
             * old method when resource is not found.
             */
            if ( result == null ) {
                classLoader = cls.getClassLoader();
                if ( classLoader != null ) {
                    result = classLoader.getResourceAsStream( name );
                }
            }
        }

        return result;
    }

    /**
     * Normalizes a path, removing double and single dot path steps.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     * <p>
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               --&gt;   /foo/
     * /foo/./              --&gt;   /foo/
     * /foo/../bar          --&gt;   /bar
     * /foo/../bar/         --&gt;   /bar/
     * /foo/../bar/../baz   --&gt;   /baz
     * //foo//./bar         --&gt;   /foo/bar
     * /../                 --&gt;   null
     * ../foo               --&gt;   null
     * foo/bar/..           --&gt;   foo/
     * foo/../../bar        --&gt;   null
     * foo/../bar           --&gt;   bar
     * //server/foo/../bar  --&gt;   //server/bar
     * //server/../bar      --&gt;   null
     * C:\foo\..\bar        --&gt;   C:\bar
     * C:\..\bar            --&gt;   null
     * ~/foo/../bar/        --&gt;   ~/bar/
     * ~/../bar             --&gt;   null
     * </pre>
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename the filename to normalize, null returns null
     * @return the normalized filename, or null if invalid
     */
    public static String normalize( final String filename ) {
        return doNormalize( filename, CLASSPATH_SEPARATOR, true );
    }

    /**
     * Create absolute filenames relative to the given class.
     * <p/>
     * For example given the following sql file:
     *
     * <code>/com/mycorp/dao/person/insert_person.sql:</code>
     *
     * <pre class="prettyprint">
     * INSERT INTO PERSON (name, age) VALUES (:name, :age);
     * </pre>
     *
     * and given that the <code>PersonDao</code> exists in the same package" <code>com.mycorp.dao.person.PersonDao</code>, create an absolute
     * filename to the insert_person.sql file as follows:
     *
     * <pre class="prettyprint">
     * // The PersonDao.class package will be converted to a path and prepended to insert_product.sql filename
     * String filename = OliveUtils.normalize(PersonDao.class, "insert_product.sql");
     * System.out.println(filename);
     * </pre>
     *
     * The result of the above <code>println</code> statement will be: <code>/com/mycorp/dao/person/insert_person.sql</code>.
     *
     * @param cls the class to create an absolute filename from
     * @param filename the filename to create an absolute filename relative to the class
     * @return the absolute filenames relative to the given class
     */
    public static String normalize( Class cls, final String filename ) {
        if ( cls == null ) {
            throw new IllegalArgumentException( "class is required!" );
        }
        if ( filename == null ) {
            return null;
        }
        if ( filename.startsWith( "/" ) ) {
            return filename;
        }
        String pkg = cls.getPackage().getName();
        pkg = pkg.replace( ".", "/" );
        String fullname = '/' + pkg + '/' + filename;

        return doNormalize( fullname, '/', true );
    }

    /**
     * Return a new XML Document for the given input stream.
     *
     * @param inputStream the input stream
     * @return new XML Document
     * @throws RuntimeException if a parsing error occurs
     */
    public static Document buildDocument( InputStream inputStream ) {
        return buildDocument( inputStream, null );
    }

    /**
     * Return a new XML Document for the given input stream and XML entity
     * resolver.
     *
     * @param inputStream the input stream
     * @param entityResolver the XML entity resolver
     * @return new XML Document
     * @throws RuntimeException if a parsing error occurs
     */
    public static Document buildDocument( InputStream inputStream,
        EntityResolver entityResolver ) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = factory.newDocumentBuilder();

            if ( entityResolver != null ) {
                builder.setEntityResolver( entityResolver );
            }

            return builder.parse( inputStream );

        } catch ( Exception ex ) {
            throw new RuntimeException( "Error parsing XML", ex );
        }
    }

    /**
     * Return the first XML child Element for the given parent Element and child
     * Element name.
     *
     * @param parent the parent element to get the child from
     * @param name the name of the child element
     * @return the first child element for the given name and parent
     */
    public static Element getChild( Element parent, String name ) {
        NodeList nodeList = parent.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node node = nodeList.item( i );
            if ( node instanceof Element ) {
                if ( node.getNodeName().equals( name ) ) {
                    return (Element) node;
                }
            }
        }
        return null;
    }

    /**
     * Return the list of XML child Element elements with the given name from
     * the given parent Element.
     *
     * @param parent the parent element to get the child from
     * @param name the name of the child element
     * @return the list of XML child elements for the given name
     */
    public static List<Element> getChildren( Element parent, String name ) {
        List<Element> list = new ArrayList<Element>();
        NodeList nodeList = parent.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node node = nodeList.item( i );
            if ( node instanceof Element ) {
                if ( node.getNodeName().equals( name ) ) {
                    list.add( (Element) node );
                }
            }
        }
        return list;
    }

    /**
     * Internal method to perform the normalization.
     *
     * @param filename the filename
     * @param separator The separator character to use
     * @param keepSeparator true to keep the final separator
     * @return the normalized filename
     */
    private static String doNormalize( final String filename, final char separator, final boolean keepSeparator ) {

        int size = filename.length();
        if ( size == 0 ) {
            return filename;
        }
        final int prefix = getPrefixLength( filename );
        if ( prefix < 0 ) {
            return null;
        }

        final char[] array = new char[size + 2];  // +1 for possible extra slash, +2 for arraycopy
        filename.getChars( 0, filename.length(), array, 0 );

        // fix separators throughout
        /*
         final char otherSeparator = separator == SYSTEM_SEPARATOR ? OTHER_SEPARATOR : SYSTEM_SEPARATOR;
         for (int i = 0; i < array.length; i++) {
         if (array[i] == otherSeparator) {
         array[i] = separator;
         }
         }*/
        // add extra separator on the end to simplify code below
        boolean lastIsDirectory = true;
        if ( array[size - 1] != separator ) {
            array[size++] = separator;
            lastIsDirectory = false;
        }

        // adjoining slashes
        for ( int i = prefix + 1; i < size; i++ ) {
            if ( array[i] == separator && array[i - 1] == separator ) {
                System.arraycopy( array, i, array, i - 1, size - i );
                size--;
                i--;
            }
        }

        // dot slash
        for ( int i = prefix + 1; i < size; i++ ) {
            if ( array[i] == separator && array[i - 1] == '.' && (i == prefix + 1 || array[i - 2] == separator) ) {
                if ( i == size - 1 ) {
                    lastIsDirectory = true;
                }
                System.arraycopy( array, i + 1, array, i - 1, size - i );
                size -= 2;
                i--;
            }
        }

        // double dot slash
        outer:
        for ( int i = prefix + 2; i < size; i++ ) {
            if ( array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' && (i == prefix + 2 || array[i - 3] == separator) ) {
                if ( i == prefix + 2 ) {
                    return null;
                }
                if ( i == size - 1 ) {
                    lastIsDirectory = true;
                }
                int j;
                for ( j = i - 4; j >= prefix; j-- ) {
                    if ( array[j] == separator ) {
                        // remove b/../ from a/b/../c
                        System.arraycopy( array, i + 1, array, j + 1, size - i );
                        size -= i - j;
                        i = j + 1;
                        continue outer;
                    }
                }
                // remove a/../ from a/../c
                System.arraycopy( array, i + 1, array, prefix, size - i );
                size -= i + 1 - prefix;
                i = prefix + 1;
            }
        }

        if ( size <= 0 ) {  // should never be less than 0
            return "";
        }
        if ( size <= prefix ) {  // should never be less than prefix
            return new String( array, 0, size );
        }
        if ( lastIsDirectory && keepSeparator ) {
            return new String( array, 0, size );  // keep trailing separator
        }
        return new String( array, 0, size - 1 );  // lose trailing separator
    }

    /**
     * Returns the length of the filename prefix, such as <code>C:/</code> or <code>~/</code>.
     * <p>
     * This method will handle a file in either Unix or Windows format.
     * <p>
     * The prefix length includes the first slash in the full filename
     * if applicable. Thus, it is possible that the length returned is greater
     * than the length of the input string.
     * <pre>
     * Windows:
     * a\b\c.txt           --&gt; ""          --&gt; relative
     * \a\b\c.txt          --&gt; "\"         --&gt; current drive absolute
     * C:a\b\c.txt         --&gt; "C:"        --&gt; drive relative
     * C:\a\b\c.txt        --&gt; "C:\"       --&gt; absolute
     * \\server\a\b\c.txt  --&gt; "\\server\" --&gt; UNC
     * \\\a\b\c.txt        --&gt;  error, length = -1
     *
     * Unix:
     * a/b/c.txt           --&gt; ""          --&gt; relative
     * /a/b/c.txt          --&gt; "/"         --&gt; absolute
     * ~/a/b/c.txt         --&gt; "~/"        --&gt; current user
     * ~                   --&gt; "~/"        --&gt; current user (slash added)
     * ~user/a/b/c.txt     --&gt; "~user/"    --&gt; named user
     * ~user               --&gt; "~user/"    --&gt; named user (slash added)
     * //server/a/b/c.txt  --&gt; "//server/"
     * ///a/b/c.txt        --&gt; error, length = -1
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is running on.
     * ie. both Unix and Windows prefixes are matched regardless.
     *
     * Note that a leading // (or \\) is used to indicate a UNC name on Windows.
     * These must be followed by a server name, so double-slashes are not collapsed
     * to a single slash at the start of the filename.
     *
     * @param filename the filename to find the prefix in, null returns -1
     * @return the length of the prefix, -1 if invalid or null
     */
    private static int getPrefixLength( final String filename ) {
        if ( filename == null ) {
            return NOT_FOUND;
        }
        final int len = filename.length();
        if ( len == 0 ) {
            return 0;
        }
        char ch0 = filename.charAt( 0 );
        if ( ch0 == ':' ) {
            return NOT_FOUND;
        }
        if ( len == 1 ) {
            if ( ch0 == '~' ) {
                return 2;  // return a length greater than the input
            }
            return isSeparator( ch0 ) ? 1 : 0;
        } else {
            if ( ch0 == '~' ) {
                int posUnix = filename.indexOf( UNIX_SEPARATOR, 1 );
                int posWin = filename.indexOf( WINDOWS_SEPARATOR, 1 );
                if ( posUnix == NOT_FOUND && posWin == NOT_FOUND ) {
                    return len + 1;  // return a length greater than the input
                }
                posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
                posWin = posWin == NOT_FOUND ? posUnix : posWin;
                return Math.min( posUnix, posWin ) + 1;
            }
            final char ch1 = filename.charAt( 1 );
            if ( ch1 == ':' ) {
                ch0 = Character.toUpperCase( ch0 );
                if ( ch0 >= 'A' && ch0 <= 'Z' ) {
                    if ( len == 2 || isSeparator( filename.charAt( 2 ) ) == false ) {
                        return 2;
                    }
                    return 3;
                }
                return NOT_FOUND;

            } else if ( isSeparator( ch0 ) && isSeparator( ch1 ) ) {
                int posUnix = filename.indexOf( UNIX_SEPARATOR, 2 );
                int posWin = filename.indexOf( WINDOWS_SEPARATOR, 2 );
                if ( posUnix == NOT_FOUND && posWin == NOT_FOUND || posUnix == 2 || posWin == 2 ) {
                    return NOT_FOUND;
                }
                posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
                posWin = posWin == NOT_FOUND ? posUnix : posWin;
                return Math.min( posUnix, posWin ) + 1;
            } else {
                return isSeparator( ch0 ) ? 1 : 0;
            }
        }
    }

    /**
     * Checks if the character is a separator.
     *
     * @param ch the character to check
     * @return true if it is a separator character
     */
    private static boolean isSeparator( final char ch ) {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
    }

    private static long copyLarge( final Reader input, final Writer output ) {
        return copyLarge( input, output, new char[DEFAULT_BUFFER_SIZE] );
    }

    private static long copyLarge( final Reader input, final Writer output, final char[] buffer ) {
        try {

            long count = 0;
            int n = 0;
            while ( EOF != (n = input.read( buffer )) ) {
                output.write( buffer, 0, n );
                count += n;
            }
            return count;

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private static void copy( final InputStream input, final Writer output, String inputEncoding ) {
        try {
            final InputStreamReader in = new InputStreamReader( input, inputEncoding );
            copy( in, output );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    private static int copy( final Reader input, final Writer output ) {
        final long count = copyLarge( input, output );
        if ( count > Integer.MAX_VALUE ) {
            return -1;
        }
        return (int) count;
    }

    public static void main( String[] args ) {
        String source = normalize( OliveUtils.class, "queries.xml" );
        InputStream is = getResourceAsStream( OliveUtils.class, source );
        System.out.println( "is" + is );
        Document document = buildDocument( is );
        Element rootElm = document.getDocumentElement();
        System.out.println( "root nodename: " + rootElm.getNodeName() );
        List< Element> queries = getChildren( rootElm, "query" );
        for ( Element query : queries ) {
            System.out.println( "name: " + query.getAttribute( "name" ) );
            String sql = query.getTextContent();
            //sql = sql.replaceAll("[\n\r]", "");
            System.out.println( "sql: " + sql );
        }
    }
}
