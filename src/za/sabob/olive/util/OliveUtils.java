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
import java.math.*;
import za.sabob.olive.ps.SqlParam;
import za.sabob.olive.ps.ParsedSql;
import za.sabob.olive.ps.SqlParams;

import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javax.sql.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.query.*;

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
     * @return RuntimeException
     */
    public static RuntimeException commitSilently( Connection conn ) {
        try {
            if ( conn != null ) {
                conn.commit();
            }
        } catch ( SQLException e ) {
            return toRuntimeException( e );
        }

        return null;
    }

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
     * Returns the connection for the given DataSource and wraps any SQLExceptions thrown in RuntimeExcepions.
     * <p/>
     *
     * @param ds the DataSource to get a Connection from
     * @return the DataSource Connection
     */
    public static Connection getConnection( DataSource ds ) {
        try {
            if ( ds == null ) {
                throw new IllegalStateException( "DataSource cannot be null" );
            }

            return ds.getConnection();
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Returns the connection for the given DataSource and set the connection#setAutoCommit to the given value. Any SQLExceptions are wrapped and thrown as
     * RuntimeExcepions.
     *
     * @param ds the DataSource to get a Connection from
     * @param autoCommit the autoCommit value to set
     * @return the DataSource Connection
     */
    public static Connection getConnection( DataSource ds, boolean autoCommit ) {
        try {
            if ( ds == null ) {
                throw new IllegalStateException( "DataSource cannot be null" );
            }

            Connection conn = ds.getConnection();

            setAutoCommit( conn, autoCommit );

            return conn;

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the connection for the given DataSource, username and password and wraps any SQLExceptions thrown in RuntimeExcepions.
     * <p/>
     *
     * @param ds the DataSource to get a Connection from
     * @param username - the database user on whose behalf the connection is being made
     * @param password - the user's password
     * @return the DataSource Connection
     */
    public static Connection getConnection( DataSource ds, String username, String password ) {
        try {
            if ( ds == null ) {
                throw new IllegalStateException( "DataSource cannot be null" );
            }

            return ds.getConnection( username, password );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the connection for the given DataSource, username and password and set the connection#setAutoCommit to the given value.
     * Any SQLExceptions are wrapped and thrown as RuntimeExcepions.
     * <p/>
     *
     * @param ds the DataSource to get a Connection from
     * @param username - the database user on whose behalf the connection is being made
     * @param password - the user's password
     * @param autoCommit the autoCommit value to set
     *
     * @return the DataSource Connection
     */
    public static Connection getConnection( DataSource ds, String username, String password, boolean autoCommit ) {
        try {
            if ( ds == null ) {
                throw new IllegalStateException( "DataSource cannot be null" );
            }

            Connection conn = ds.getConnection( username, password );

            setAutoCommit( conn, autoCommit );

            return conn;

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the connection from the DriverManager.class for the given url and wraps any SQLExceptions thrown in RuntimeExcepions.
     *
     * @param url - a database url of the form jdbc:subprotocol:subname
     *
     * @return a connection to the URL
     */
    public static Connection getConnection( String url ) {
        try {
            if ( isBlank( url ) ) {
                throw new IllegalStateException( "url cannot be empty" );
            }

            return DriverManager.getConnection( url );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the connection from the DriverManager.class for the given url and wraps any SQLExceptions thrown in RuntimeExcepions.
     *
     * @param url - a database url of the form jdbc:subprotocol:subname
     * @param info - a list of arbitrary string tag/value pairs as connection arguments; normally at least a "user" and "password" property
     * should be included
     *
     * @return a connection to the URL
     */
    public static Connection getConnection( String url, Properties info ) {
        try {
            if ( isBlank( url ) ) {
                throw new IllegalStateException( "url cannot be empty" );
            }

            return DriverManager.getConnection( url, info );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the connection from the DriverManager.class for the given url and wraps any SQLExceptions thrown in RuntimeExcepions.
     *
     * @param url - a database url of the form jdbc:subprotocol:subname
     * @param user - the database user on whose behalf the connection is being made
     * @param password - the user's password
     *
     * @return a connection to the URL
     */
    public static Connection getConnection( String url, String user, String password ) {
        try {
            if ( isBlank( url ) ) {
                throw new IllegalStateException( "url cannot be empty" );
            }

            return DriverManager.getConnection( url, user, password );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Rollback the given connection and return the given exception as a RuntimeException.
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
     * throw OliveUtils.rollback(conn, e);
     * } finally {
     *
     *     // Close resources and switch autoCommit back to true
     * OliveUtils.close(true, ps, conn);
     * }
     *
     * </pre>
     *
     * @param conn the connection to rollback
     * @param exception the Exception that is causing the transaction to be rolled back
     * @return RuntimeException
     */
    public static RuntimeException rollback( Connection conn, Exception exception ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            exception = addSuppressed( e, exception );
        }

        return toRuntimeException( exception );
    }

    /**
     * Rollback the given connection and throws any exception that occurs while rolling back as a RuntimeException.
     * <p/>
     * This method is null safe, so the connection and sqlException can be null.
     *
     * @param conn the connection to rollback
     */
    public static void rollback( Connection conn ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Rollback the given connection and logs any exception that occurs while rolling back without throwing an exception.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param conn the connection to rollback
     *
     * @return the given exception and any exception that occurred while rolling back the connection
     */
    public static RuntimeException rollbackSilently( Connection conn ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            return toRuntimeException( e );
        }

        return null;
    }

    /**
     * Rollback the given connection and returns any exception that occurs while rolling back without throwing an exception.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param conn the connection to rollback
     * @param exception the Exception that is causing the transaction to be rolled back
     *
     * @return the given exception and any exception that occurred while rolling back the connection
     */
    public static RuntimeException rollbackSilently( Connection conn, Exception exception ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            exception = addSuppressed( e, exception );
        }

        return toRuntimeException( exception );
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
     * Closes the given connection and wraps any SQLExceptions thrown as RuntimeExcepions. Also sets the Connection#setAutoCommit to the
     * given value.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param autoCommit whether to enable/disable Connection#setAutoCommit
     * @param conn the connection to close
     */
    public static void closeConnection( boolean autoCommit, Connection conn ) {

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            setAutoCommit( conn, autoCommit );

        } catch ( RuntimeException e ) {
            mainException = e;
        }

        try {

            closeConnection( conn );

        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        throwAsRuntimeIfException( mainException );
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

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            closeStatement( st );

        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );

        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given resultSet and statement and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * <b>Note:</b> only the first exception thrown by closing the resultSet and statement will be thrown as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the statement and connection can be null.
     *
     * @param rs the resultSet to close
     * @param st the statement to close
     */
    public static void close( ResultSet rs, Statement st ) {

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            closeResultSet( rs );
        } catch ( RuntimeException e ) {

            mainException = e;
        }

        try {
            closeStatement( st );

        } catch ( RuntimeException e ) {

            mainException = addSuppressed( e, mainException );
        }

        throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given statement and connection and wraps any SQLExceptions thrown as RuntimeExcepions. Also sets the Connection#setAutoCommit to the
     * given value.
     * <p/>
     * s
     * <b>Note:</b> only the first exception thrown by setting Connection#autoCommit, closing the statement or closing the connection will be thrown
     * as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the statement and connection can be null.
     *
     * @param autoCommit whether to enable/disable Connection#setAutoCommit
     * @param st the statement to close
     * @param conn the connection to close
     */
    public static void close( boolean autoCommit, Statement st, Connection conn ) {

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            setAutoCommit( conn, autoCommit );

        } catch ( RuntimeException e ) {
            mainException = e;
        }

        try {
            closeStatement( st );
        } catch ( RuntimeException e ) {

            mainException = addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );

        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        throwAsRuntimeIfException( mainException );
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

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            closeResultSet( rs );

        } catch ( RuntimeException e ) {
            mainException = e;
        }

        try {
            closeStatement( st );
        } catch ( RuntimeException e ) {

            mainException = addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );
        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given resultSet, statement and connection and wraps any SQLExceptions thrown as RuntimeExcepions. Also sets the Connection#setAutoCommit to the
     * given value.
     * <p/>
     * <b>Note:</b> only the first exception thrown by setting autoCommit(false), closing the resultset, statement and connection will be thrown as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the resultset, statement and connection can be null.
     *
     * @param autoCommit whether to enable/disable Connection#setAutoCommit
     * @param rs the resultset to close
     * @param st the statement to close
     * @param conn the connection to close
     */
    public static void close( boolean autoCommit, ResultSet rs, Statement st, Connection conn ) {

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            setAutoCommit( conn, autoCommit );

        } catch ( RuntimeException e ) {
            mainException = e;
        }

        try {
            closeResultSet( rs );

        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        try {
            closeStatement( st );
        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );
        } catch ( RuntimeException e ) {
            mainException = addSuppressed( e, mainException );
        }

        throwAsRuntimeIfException( mainException );
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
     * Closes the given connection and wraps any SQLExceptions thrown as RuntimeExcepions. Also sets the Connection#setAutoCommit to the
     * given value.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param autoCommit whether to enable/disable Connection#setAutoCommit
     * @param conn the connection to close
     */
    public static void close( boolean autoCommit, Connection conn ) {
        closeConnection( autoCommit, conn );
    }

    /**
     * Sets the connection autoCommit mode and and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param conn the connection on which to set autoCommit
     * @param bool true to enable setAutoCommit, false to disable autoCommit
     *
     * @throws RuntimeException that wraps the SQLException
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

    public static boolean getAutoCommit( Connection conn ) {

        try {
            if ( conn == null ) {
                return false;
            }

            return conn.getAutoCommit();

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static void setTransactionIsolation( Connection conn, int level ) {

        try {
            if ( conn == null ) {
                return;
            }

            conn.setTransactionIsolation( level );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static int getTransactionIsolation( Connection conn ) {

        try {
            if ( conn == null ) {
                return -1;
            }

            return conn.getTransactionIsolation();

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Returns the generated key and wraps any SQLExceptions thrown as a RuntimeExcepion.
     *
     * This method is null safe, so the statement can be null.
     *
     * @param st the statement from which to get the generatedKey
     *
     * @return the generated key or null if no key was generated
     */
    public static long getGeneratedKey( Statement st ) {

        ResultSet rs = null;
        try {
            long id = 0;

            rs = st.getGeneratedKeys();

            if ( rs.next() ) {
                id = rs.getLong( 1 );
            }

            return id;

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        } finally {
            close( rs );
        }
    }

    /**
     * Returns the generated Keys and wraps any SQLExceptions thrown as a RuntimeExcepion.
     *
     * This method is null safe, so the statement can be null.
     *
     * @param st the statement from which to get the generatedKeys
     *
     * @return the list of generated keys as long values
     */
    public static List<Long> getGeneratedKeys( Statement st ) {

        ResultSet rs = null;

        try {
            rs = st.getGeneratedKeys();

            List list = new ArrayList();

            while ( rs.next() ) {
                long id = rs.getLong( 1 );
                list.add( id );
            }

            return list;

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            close( rs );
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

    public static PreparedStatement prepareStatement( JDBCContext ctx, ParsedSql parsedSql, SqlParams parameters ) {
        Connection conn = ctx.getConnection();
        PreparedStatement ps = prepareStatement( conn, parsedSql, parameters );
        ctx.add( ps );
        return ps;        
    }

    public static PreparedStatement prepareStatement( Connection conn, String sql ) {

        try {
            PreparedStatement ps = conn.prepareStatement( sql );
            return ps;

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static PreparedStatement prepareStatement( JDBCContext ctx, String sql ) {
        Connection conn = ctx.getConnection();
        PreparedStatement ps = prepareStatement( conn, sql );
        ctx.add( ps );
        return ps;
    }

    /**
     * Create and return a PreparedStatement for the given connection, parsedSql, parameters and options.
     * <p/>
     * the PreparedStatement will have all it's named parameters replaced by the given parameters
     *
     * @param conn the connection to create the PreparedStatement with
     * @param parsedSql the parsed representation of the SQL statement
     * @param parameters the source for named parameters
     * @param autoGeneratedKeys specifies the autoGenerated keys value of: Statement.RETURN_GENERATED_KEYS or Statement.NO_GENERATED_KEYS
     * @return the PreparedStatement with all named parameters replaced by the given parameters
     */
    public static PreparedStatement prepareStatement( Connection conn, ParsedSql parsedSql, SqlParams parameters, int autoGeneratedKeys ) {
        String sql = NamedParameterUtils.substituteNamedParameters( parsedSql, parameters );
        try {
            PreparedStatement ps = conn.prepareStatement( sql, autoGeneratedKeys );
            setParams( ps, parsedSql, parameters );
            return ps;

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static PreparedStatement prepareStatement( JDBCContext ctx, ParsedSql parsedSql, SqlParams parameters, int autoGeneratedKeys ) {
        Connection conn = ctx.getConnection();
        PreparedStatement ps = prepareStatement( conn, parsedSql, parameters, autoGeneratedKeys );
        ctx.add( ps );
        return ps;
    }

    /**
     * Create and return a PreparedStatement for the given connection, parsedSql, parameters and options.
     * <p/>
     * the PreparedStatement will have all it's named parameters replaced by the given parameters
     *
     * @param conn the connection to create the PreparedStatement with
     * @param parsedSql the parsed representation of the SQL statement
     * @param parameters the source for named parameters
     * @param resultSetType - one of the following ResultSet constants: ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency - one of the following ResultSet constants: ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability - one of the following ResultSet constants: ResultSet.HOLD_CURSORS_OVER_COMMIT or ResultSet.CLOSE_CURSORS_AT_COMMIT
     * @return the PreparedStatement with all named parameters replaced by the given parameters
     */
    public static PreparedStatement prepareStatement( Connection conn, ParsedSql parsedSql, SqlParams parameters, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability ) {
        String sql = NamedParameterUtils.substituteNamedParameters( parsedSql, parameters );
        try {
            PreparedStatement ps = conn.prepareStatement( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
            setParams( ps, parsedSql, parameters );
            return ps;

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static PreparedStatement prepareStatement( JDBCContext ctx, ParsedSql parsedSql, SqlParams parameters, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability ) {
        Connection conn = ctx.getConnection();
        PreparedStatement ps = prepareStatement( conn, parsedSql, parameters, resultSetType, resultSetConcurrency, resultSetHoldability );
        ctx.add( ps );
        return ps;
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

    public static PreparedStatement prepareStatement( JDBCContext ctx, String sqlStatement, SqlParams parameters ) {
        Connection conn = ctx.getConnection();
        PreparedStatement ps = prepareStatement( conn, sqlStatement, parameters );
        ctx.add( ps );
        return ps;
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
            close( ps );
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
    public static String normalize( String filename ) {
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
     * @param relative the class to create an absolute filename from
     * @param filename the filename to create an absolute filename relative to the class
     * @return the absolute filenames relative to the given class
     */
    public static String normalize( Class relative, String filename ) {
        if ( relative == null ) {
            throw new IllegalArgumentException( "class is required!" );
        }
        if ( filename == null ) {
            return null;
        }
        if ( filename.startsWith( "/" ) ) {
            return filename;
        }
        String pkg = relative.getPackage().getName();
        pkg = pkg.replace( ".", "/" );
        String fullname = '/' + pkg + '/' + filename;

        return doNormalize( fullname, '/', true );
    }

    /**
     * Create absolute filenames relative to the given object' class.
     * <p/>
     * This method delegates to {@link #normalize(java.lang.Class, java.lang.String)}.
     *
     * @param relative the object which class to use to create an absolute filename from
     * @param filename the filename to create an absolute filename relative to the class
     * @return the absolute filenames relative to the given class
     */
    public static String normalize( Object relative, String filename ) {
        if ( relative == null ) {
            throw new IllegalArgumentException( "relative object cannot be null" );
        }

        String normalizedFilename = normalize( relative.getClass(), filename );

        return normalizedFilename;

    }

    /**
     * Create absolute filenames relative to the given class.
     * <p/>
     * This method delegates to {@link #normalize(java.lang.Class, java.lang.String)}.
     *
     * @param relative the class to create an absolute filename from
     * @param filename the filename to create an absolute filename relative to the class
     * @return the absolute filenames relative to the given class
     */
    public static String path( Class relative, String filename ) {
        String normalizedFilename = normalize( relative, filename );
        return normalizedFilename;
    }

    /**
     * Create absolute filenames relative to the given object' class.
     * <p/>
     * This method delegates to {@link #normalize(java.lang.Object, java.lang.String)}.
     *
     * @param relative the object which class to use to create an absolute filename from
     * @param filename the filename to create an absolute filename relative to the class
     * @return the absolute filenames relative to the given class
     */
    public static String path( Object relative, String filename ) {
        String normalizedFilename = normalize( relative, filename );
        return normalizedFilename;
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
     * Returns true if the object is an array.
     * <p>
     * This method will return true if the argument is an Object array or a primitive array.
     *
     * @param obj the object to check if it is an array or not
     * @return true if the object is an array, false otherwise
     */
    public static boolean isArray( Object obj ) {

        if ( obj == null ) {
            return false;
        }

        if ( isObjectArray( obj ) ) {
            return true;
        }

        return isPrimitiveArray( obj );
    }

    /**
     * Returns true if the object is an array of Objects Object[], Integer[] etc. Array of primitives will return false.
     * <p>
     * s
     * This method will return true if the argument is an array of Objects.
     *
     * @param obj the object to check if it is an array of Objects or not
     * @return true if the object is an array of Objects, false otherwise
     */
    public static boolean isObjectArray( Object obj ) {

        if ( obj == null ) {
            return false;
        }

        if ( obj instanceof Object[] ) {
            return true;
        }

        return false;

    }

    /**
     * Returns true if the object is an array of primitives int[], boolean[] etc. Array of Objects will return false.
     * <p>
     * s
     * This method will return true if the argument is an array of primitives.
     *
     * @param obj the object to check if it is an array of primitives or not
     * @return true if the object is an array of primitives, false otherwise
     */
    public static boolean isPrimitiveArray( Object obj ) {

        if ( obj == null ) {
            return false;
        }

        if ( isObjectArray( obj ) ) {
            return false;
        }

        return obj.getClass().isArray();
    }

    /**
     * Return the length of the given obj, or 0 if the obj is not an array.
     *
     * @param obj the array which length to return
     * @return the length of the given array
     */
    public static int getArrayLength( Object obj ) {

        if ( isArray( obj ) ) {
            return java.lang.reflect.Array.getLength( obj );
        }

        return 0;
    }

    /**
     * Convert the given array to a list recursively ie each array contained in the given array are also converted to a list.
     *
     * @param array the array to convert into a list
     * @return the converted list
     * @throws IllegalArgumentException if array is not an actual array
     */
    public static List toList( Object array ) {

        if ( !isArray( array ) ) {
            throw new IllegalArgumentException( "object must be an array" );
        }

        List list = new ArrayList();

        int length = java.lang.reflect.Array.getLength( array );

        for ( int i = 0; i < length; i++ ) {

            Object arrayItem = java.lang.reflect.Array.get( array, i );

            if ( isObjectArray( arrayItem ) ) {
                list.add( (Object[]) arrayItem );

            } else if ( isPrimitiveArray( arrayItem ) ) {

                List innerList = toSimpleList( arrayItem );
                list.add( innerList );

            } else {
                list.add( arrayItem );

            }
        }

        return list;
    }

    /**
     * Adds the given suppressedException to the mainException and returns the mainException, unless it is null, in which case the suppressedException is
     * returned.
     *
     * @param mainException the main exception on which to add the suppressedException
     * @param supressedException the exception to add to the mainException
     * @return the mainException or supresesdException if mainException is null
     */
    public static Exception addSuppressed( Exception mainException, Exception supressedException ) {

        if ( supressedException == null ) {
            return mainException;
        }

        if ( mainException == null ) {
            return supressedException;
        }

        if ( mainException == supressedException ) {
            return mainException;
        }

        mainException.addSuppressed( supressedException );
        return mainException;
    }

    /**
     * Throws the given exception as a RuntimeException, unless the exception is null, in which case the method simply returns.
     *
     * @param exception the exception to throw as a RuntimeException
     */
    public static void throwAsRuntimeIfException( Exception exception ) {

        if ( exception == null ) {
            return;
        }

        if ( exception instanceof RuntimeException ) {
            throw (RuntimeException) exception;
        }

        throw new RuntimeException( exception );
    }

    public static RuntimeException toRuntimeException( Exception exception ) {
        if ( exception == null ) {
            return null;
        }

        if ( exception instanceof RuntimeException ) {
            return (RuntimeException) exception;
        }
        return new RuntimeException( exception );

    }

    /**
     * Closes the given list of autoCloseabes and wraps any Exceptions thrown as RuntimeExcepions. Every closeable will have its #close method called, regardless
     * if an exception is thrown.
     * <p/>
     * <b>Note:</b> exceptions thrown by the autoClosable objects will be chained using Throwable#addSuppressed(Throwable).
     * <p/>
     *
     * If an exception is thrown by one or more of the autoClosables, a RuntimeException is thrown wrapping the exceptions.
     * <p/>
     * This method is null safe, so C;loseables can be null.
     *
     * @param closeables the list of closeables to close
     */
    public static void close( Iterable<? extends AutoCloseable> closeables ) {

        if ( closeables == null ) {
            return;
        }

        // Main exception to assign other exceptions to
        Exception mainException = null;

        for ( final AutoCloseable closeable : closeables ) {

            if ( closeable == null ) {
                continue;
            }

            try {
                closeable.close();
            } catch ( final Exception ex ) {
                mainException = addSuppressed( ex, mainException );
            }
        }

        throwAsRuntimeIfException( mainException );
    }

    public static RuntimeException closeSilently( boolean autoCommit, Exception exception, AutoCloseable... closeables ) {

        List list = Arrays.asList( closeables );
        return closeSilently( autoCommit, exception, list );
    }

    public static RuntimeException closeSilently( boolean autoCommit, Exception exception, Iterable<? extends AutoCloseable> closeables ) {

        try {
            close( autoCommit, closeables );
            return OliveUtils.toRuntimeException( exception );

        } catch ( RuntimeException ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
            return OliveUtils.toRuntimeException( exception );
        }
    }

    public static RuntimeException closeSilently( Exception exception, Iterable<? extends AutoCloseable> closeables ) {

        try {
            close( closeables );
            return OliveUtils.toRuntimeException( exception );

        } catch ( RuntimeException ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
            return OliveUtils.toRuntimeException( exception );
        }
    }

    public static RuntimeException closeSilently( boolean autoCommit, Iterable<? extends AutoCloseable> closeables ) {
        return closeSilently( autoCommit, null, closeables );
    }

    public static RuntimeException closeSilently( boolean autoCommit, AutoCloseable... closeables ) {
        return closeSilently( autoCommit, null, closeables );
    }

    public static RuntimeException closeSilently( Iterable<? extends AutoCloseable> closeables ) {
        return closeSilently( null, closeables );
    }

    public static RuntimeException closeSilently( Exception exception, AutoCloseable... closeables ) {

        List list = Arrays.asList( closeables );
        return closeSilently( exception, list );
    }

    public static RuntimeException closeSilently( AutoCloseable... closeables ) {

        return closeSilently( null, closeables );
    }

    /**
     * Closes the given list of autoCloseabes and wraps any Exceptions thrown as RuntimeExcepions. Every closeable will have its #close method called, regardless
     * if an exception is thrown. Any connections will have their setCommit status set to the given value.
     * <p/>
     * <b>Note:</b> exceptions thrown by the autoClosable objects will be chained using Throwable#addSuppressed(Throwable).
     * <p/>
     *
     * If an exception is thrown by one or more of the autoClosables, a RuntimeException is thrown wrapping the exceptions.
     * <p/>
     * This method is null safe, so Closeables can be null.
     *
     * @param autoCommit set the connections autoCommit to the given value
     * @param closeables the list of closeables to close
     */
    public static void close( boolean autoCommit, Iterable<? extends AutoCloseable> closeables ) {

        if ( closeables == null ) {
            return;
        }

        // Main exception to assign other exceptions to
        Exception mainException = null;

        for ( final AutoCloseable closeable : closeables ) {

            if ( closeable == null ) {
                continue;
            }

            if ( closeable instanceof Connection ) {
                Connection conn = (Connection) closeable;

                try {
                    conn.setAutoCommit( autoCommit );

                } catch ( SQLException ex ) {
                    mainException = addSuppressed( ex, mainException );
                }

            }

            try {
                closeable.close();
            } catch ( final Exception ex ) {
                mainException = addSuppressed( ex, mainException );
            }
        }

        throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given autoCloseabe array and wraps any Exceptions thrown as RuntimeExcepions. Every closeable will have its #close method called, regardless
     * if an exception is thrown.
     * <p/>
     * <b>Note:</b> exceptions thrown by the autoClosable objects will be chained using Throwable#addSuppressed(Throwable).
     * <p/>
     *
     * If an exception is thrown by one or more of the autoClosables, a RuntimeException is thrown wrapping the exceptions.
     * <p/>
     * This method is null safe, so Closeables can be null.
     *
     * @param closeables the closeable array to close
     */
    public static void close( AutoCloseable... closeables ) {

        if ( closeables == null || closeables.length == 0 ) {
            return;
        }

        List list = Arrays.asList( closeables );
        close( list );
    }

    /**
     * Closes the given autoCloseabe array and wraps any Exceptions thrown as RuntimeExcepions. Every closeable will have its #close method called, regardless
     * if an exception is thrown.
     * <p/>
     * <b>Note:</b> exceptions thrown by the autoClosable objects will be chained using Throwable#addSuppressed(Throwable).
     * <p/>
     *
     * If an exception is thrown by one or more of the autoClosables, a RuntimeException is thrown wrapping the exceptions.
     * <p/>
     * This method is null safe, so Closeables can be null.
     *
     * @param autoCommit set the connections autoCommit to the given value
     * @param closeables the closeable array to close
     */
    public static void close( boolean autoCommit, AutoCloseable... closeables ) {

        if ( closeables == null || closeables.length == 0 ) {
            return;
        }

        List list = Arrays.asList( closeables );
        close( autoCommit, list );
    }

    public static Connection findConnection( AutoCloseable... closeables ) {

        if ( closeables != null && closeables.length > 0 ) {

            for ( final AutoCloseable closeable : closeables ) {
                if ( closeable instanceof Connection ) {
                    return (Connection) closeable;
                }

            }
        }
        return null;
    }

    public static List<Connection> findConnections( Iterable<? extends AutoCloseable> closeables ) {

        List<Connection> connections = new ArrayList();

        if ( closeables != null ) {

            for ( final AutoCloseable closeable : closeables ) {

                if ( closeable instanceof Connection ) {
                    connections.add( (Connection) closeable );
                }
            }
        }

        return connections;
    }

    public static List<Connection> getConnections( AutoCloseable... closeables ) {
        List list = Arrays.asList( closeables );
        return findConnections( list );
    }

    public static List<AutoCloseable> removeConnections( Iterable<? extends AutoCloseable> closeables ) {

        List connections = new ArrayList();

        if ( closeables != null ) {

            for ( final AutoCloseable closeable : closeables ) {

                if ( !(closeable instanceof Connection) ) {
                    connections.add( closeable );
                }
            }
        }

        return connections;
    }

    public static List<AutoCloseable> removeConnections( AutoCloseable... closeables ) {
        List list = Arrays.asList( closeables );
        return removeConnections( list );
    }

    /**
     * Converts the given array obj to a list, unless the obj is not an array, in which case an exception is thrown.
     *
     * @param obj the given array to convert to a list
     * @return the obj array as a list
     */
    private static List toSimpleList( Object obj ) {

        if ( !isArray( obj ) ) {
            throw new IllegalArgumentException( "object must be an array" );
        }

        List list = new ArrayList();

        int length = java.lang.reflect.Array.getLength( obj );

        for ( int i = 0; i < length; i++ ) {

            Object arrayItem = java.lang.reflect.Array.get( obj, i );
            list.add( arrayItem );
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
//
//    public static int executeUpdate( Connection conn, PreparedStatement ps ) {
//        StatementContainer stContainer = getStatementContainer( conn, ps );
//
//        int result;
//
//        try {
//
//            result = ps.executeUpdate();
//
//        } catch ( SQLException ex ) {
//            throw new RuntimeException( ex );
//        }
//
//        return result;
//    }

    public static <T> T mapToBean( PreparedStatement ps, RowMapper<T> mapper ) {
        //StatementContainer stContainer = getStatementContainer( conn, ps );

        ResultSet rs = null;

        try {
            rs = ps.executeQuery();

            while ( rs.next() ) {

                int rowNum = rs.getRow();

                T t = mapper.map( rs, rowNum );
                return t;
            }

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        } finally {
            close( rs );
        }

        return null;
    }

//    public static boolean execute( PreparedStatement ps ) {
//
//        //StatementContainer stContainer = getStatementContainer( conn, ps );
//
//        ResultSet rs = null;
//
//        try {
//
//            boolean result = ps.execute();
//            return result;
//
//        } catch ( SQLException ex ) {
//            throw new RuntimeException( ex );
//        } finally {
//            close( rs );
//        }
//    }
    public static <T> List<T> mapToList( PreparedStatement ps, RowMapper<T> mapper ) {

        ResultSet rs = null;
        List<T> list = new ArrayList<>();

        try {

            rs = ps.executeQuery();

            while ( rs.next() ) {

                int rowNum = rs.getRow();

                T t = mapper.map( rs, rowNum );
                list.add( t );
            }

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        } finally {
            close( rs );
        }

        return list;
    }

    public static <T> List<T> mapToBeans( PreparedStatement ps, RowMapper<T> mapper ) {
        return mapToList( ps, mapper );
    }

    public static <T> T mapToPrimitive( Class<T> cls, PreparedStatement ps ) {
        
        ResultSet rs = null;

        try {

             rs = ps.executeQuery();
            if ( rs.next() ) {

                if ( cls == int.class || cls == Integer.class ) {
                    Integer value = rs.getInt( 1 );
                    return (T) value;

                } else if ( cls == long.class || cls == Long.class ) {
                    Long value = rs.getLong( 1 );
                    return (T) value;

                } else if ( cls == byte.class || cls == Byte.class ) {
                    Byte value = rs.getByte( 1 );
                    return (T) value;

                } else if ( cls == boolean.class || cls == Boolean.class ) {
                    Boolean value = rs.getBoolean( 1 );
                    return (T) value;

                } else if ( cls == double.class || cls == Double.class ) {
                    Double value = rs.getDouble( 1 );
                    return (T) value;

                } else if ( cls == float.class || cls == Float.class ) {
                    Float value = rs.getFloat( 1 );
                    return (T) value;

                } else if ( cls == short.class || cls == Short.class ) {
                    Short value = rs.getShort( 1 );
                    return (T) value;

                } else if ( cls == String.class ) {
                    String value = rs.getString( 1 );
                    return (T) value;

                } else if ( cls == java.util.Date.class ) {
                    java.util.Date value = rs.getTimestamp( 1 );
                    return (T) value;

                } else if ( cls == BigDecimal.class ) {
                    BigDecimal value = rs.getBigDecimal( 1 );
                    return (T) value;
                }
            }

            throw new IllegalArgumentException( cls + " is not a supported type" );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );

        } finally {
            close( rs );
            
        }

    }

    public static ResultSet query( JDBCContext ctx, PreparedStatement ps ) {

        ResultSet rs = null;

        try {

            rs = ps.executeQuery();
            ctx.add( rs );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }

        return rs;
    }
}
