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
package za.sabob.olive.jdbc.util;

import za.sabob.olive.Mode;
import za.sabob.olive.config.OliveConfig;
import za.sabob.olive.jdbc.JDBCContext;
import za.sabob.olive.jdbc.ps.ParsedSql;
import za.sabob.olive.jdbc.ps.SqlParam;
import za.sabob.olive.jdbc.ps.SqlParams;
import za.sabob.olive.query.RowMapper;
import za.sabob.olive.util.Cache;
import za.sabob.olive.util.OliveUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Provides common JDBC utilities.
 */
public class JDBCUtils {

    /**
     * Logger instance for logging messages.
     */
    private static final Logger LOGGER = Logger.getLogger( JDBCUtils.class.getName() );

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
    public static RuntimeException commitQuietly( Connection conn ) {
        try {
            if ( conn != null ) {
                conn.commit();
            }
        } catch ( SQLException e ) {
            return OliveUtils.toRuntimeException( e );
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
     * Returns the connection for the given DataSource, username and password and wraps any SQLExceptions thrown in RuntimeExcepions.
     * <p/>
     *
     * @param ds       the DataSource to get a Connection from
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
     * @param ds         the DataSource to get a Connection from
     * @param username   - the database user on whose behalf the connection is being made
     * @param password   - the user's password
     * @param autoCommit the autoCommit value to set
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
     * @return a connection to the URL
     */
    public static Connection getConnection( String url ) {
        try {
            if ( OliveUtils.isBlank( url ) ) {
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
     * @param url  - a database url of the form jdbc:subprotocol:subname
     * @param info - a list of arbitrary string tag/value pairs as connection arguments; normally at least a "user" and "password" property
     *             should be included
     * @return a connection to the URL
     */
    public static Connection getConnection( String url, Properties info ) {
        try {
            if ( OliveUtils.isBlank( url ) ) {
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
     * @param url      - a database url of the form jdbc:subprotocol:subname
     * @param user     - the database user on whose behalf the connection is being made
     * @param password - the user's password
     * @return a connection to the URL
     */
    public static Connection getConnection( String url, String user, String password ) {
        try {
            if ( OliveUtils.isBlank( url ) ) {
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
     * @param conn      the connection to rollback
     * @param exception the Exception that is causing the transaction to be rolled back
     * @return RuntimeException
     */
    public static void rollbackAndThrow( Connection conn, Exception exception ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            exception = OliveUtils.addSuppressed( e, exception );
        }

        OliveUtils.throwAsRuntimeIfException( exception );
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
     * @return the given exception and any exception that occurred while rolling back the connection
     */
    public static RuntimeException rollbackQuietly( Connection conn ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            return OliveUtils.toRuntimeException( e );
        }

        return null;
    }

    /**
     * Rollback the given connection and returns any exception that occurs while rolling back without throwing an exception.
     * <p/>
     * This method is null safe, so the connection can be null.
     *
     * @param conn      the connection to rollback
     * @param exception the Exception that is causing the transaction to be rolled back
     * @return the given exception and any exception that occurred while rolling back the connection
     */
    public static RuntimeException rollbackQuietly( Connection conn, Exception exception ) {

        try {

            if ( conn != null ) {
                conn.rollback();
            }

        } catch ( SQLException e ) {
            exception = OliveUtils.addSuppressed( e, exception );
        }

        return OliveUtils.toRuntimeException( exception );
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
     * @param conn       the connection to close
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
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given statement and connection and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * <b>Note:</b> only the first exception thrown by closing the statement and connection will be thrown as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the statement and connection can be null.
     *
     * @param st   the statement to close
     * @param conn the connection to close
     */
    public static void close( Statement st, Connection conn ) {

        // Main exception to assign other exceptions to
        Exception mainException = null;

        try {
            closeStatement( st );

        } catch ( RuntimeException e ) {
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );

        } catch ( RuntimeException e ) {
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
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

            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
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
     * @param st         the statement to close
     * @param conn       the connection to close
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

            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );

        } catch ( RuntimeException e ) {
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given resultset, statement and connection and wraps any SQLExceptions thrown as RuntimeExcepions.
     * <p/>
     * <b>Note:</b> only the first exception thrown by closing the resultset, statement and connection will be thrown as a RuntimeExcepion.
     * <p/>
     * This method is null safe, so the resultset, statement and connection can be null.
     *
     * @param rs   the resultset to close
     * @param st   the statement to close
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

            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );
        } catch ( RuntimeException e ) {
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
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
     * @param rs         the resultset to close
     * @param st         the statement to close
     * @param conn       the connection to close
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
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        try {
            closeStatement( st );
        } catch ( RuntimeException e ) {
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        try {
            closeConnection( conn );
        } catch ( RuntimeException e ) {
            mainException = OliveUtils.addSuppressed( e, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
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
     * @param conn       the connection to close
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

            return conn.getAutoCommit();

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Returns the connection for the given DataSource and set the connection#setAutoCommit to the given value. Any SQLExceptions are wrapped and thrown as
     * RuntimeExcepions.
     *
     * @param ds               the DataSource to get a Connection from
     * @param beginTransaction if true, sets autoCommit to false, false otherwise
     * @return the DataSource Connection
     */
    public static Connection getConnection( DataSource ds, boolean beginTransaction ) {

        if ( ds == null ) {
            throw new IllegalStateException( "DataSource cannot be null" );
        }

        Connection conn = null;
        boolean currentAutoCommit = true;

        try {
            conn = JDBCUtils.getConnection( ds );
            currentAutoCommit = conn.getAutoCommit();
            boolean autoCommit = !beginTransaction;
            JDBCUtils.setAutoCommit( conn, autoCommit );
            return conn;

        } catch ( Exception e ) {
            // Restore autoCommit to previous value
            RuntimeException re = JDBCUtils.closeQuietly( currentAutoCommit, e, conn );
            throw re;
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
     * <p>
     * This method is null safe, so the statement can be null.
     *
     * @param st the statement from which to get the generatedKey
     * @return the generated key or null if no key was generated
     */
    public static long getGeneratedKey( Statement st ) {

        ResultSet rs = null;

        Exception exception = null;

        try {
            long id = 0;

            rs = st.getGeneratedKeys();

            if ( rs.next() ) {
                id = rs.getLong( 1 );
            }

            return id;

        } catch ( Exception e ) {
            exception = e;
            throw new RuntimeException( exception );

        } finally {
            exception = OliveUtils.closeQuietly( exception, rs );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    /**
     * Returns the generated Keys and wraps any SQLExceptions thrown as a RuntimeExcepion.
     * <p>
     * This method is null safe, so the statement can be null.
     *
     * @param st the statement from which to get the generatedKeys
     * @return the list of generated keys as long values
     */
    public static List<Long> getGeneratedKeys( Statement st ) {

        ResultSet rs = null;

        Exception exception = null;

        try {
            rs = st.getGeneratedKeys();

            List list = new ArrayList();

            while ( rs.next() ) {
                long id = rs.getLong( 1 );
                list.add( id );
            }

            return list;

        } catch ( Exception e ) {
            exception = e;
            throw new RuntimeException( e );

        } finally {
            exception = OliveUtils.closeQuietly( exception, rs );
            OliveUtils.throwAsRuntimeIfException( exception );
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

    public ParsedSql reparseSql( String name, String sqlStr ) {

        if ( name == null ) {
            throw new IllegalArgumentException( "name cannot be null!" );
        }

        ParsedSql parsedSql = JDBCUtils.parseSql( sqlStr );

        if ( OliveConfig.getMode() == Mode.PRODUCTION ) {
            Cache.getInstance().putParsedSql( name, parsedSql );
        }

        return parsedSql;
    }

    public ParsedSql parseSql( String name, String sqlStr ) {

        if ( name == null ) {
            throw new IllegalArgumentException( "name cannot be null!" );
        }

        if ( OliveConfig.getMode() == Mode.PRODUCTION ) {
            ParsedSql parsedSql = Cache.getInstance().getParsedSql( name );

            if ( parsedSql != null ) {
                return parsedSql;
            }
        }

        ParsedSql parsedSql = parseSql( name, sqlStr );

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
     *
     * @param parsedSql  the parsed representation of the SQL statement
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
     * @param conn       the connection to create the PreparedStatement with
     * @param parsedSql  the parsed representation of the SQL statement
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

    public static Statement createStatement( JDBCContext ctx ) {

        try {
            Connection conn = ctx.getConnection();
            Statement st = conn.createStatement();
            ctx.add( st );
            return st;
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static Statement createStatement( JDBCContext ctx, int resultSetType, int resultSetConcurrency ) {

        try {
            Connection conn = ctx.getConnection();
            Statement st = conn.createStatement( resultSetType, resultSetConcurrency );

            ctx.add( st );
            return st;
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static Statement createStatement( JDBCContext ctx, int resultSetType, int resultSetConcurrency, int resultSetHoldability ) {

        try {
            Connection conn = ctx.getConnection();
            Statement st = conn.createStatement( resultSetType, resultSetConcurrency, resultSetHoldability );

            ctx.add( st );
            return st;
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Create and return a PreparedStatement for the given connection, parsedSql, parameters and options.
     * <p/>
     * the PreparedStatement will have all it's named parameters replaced by the given parameters
     *
     * @param conn              the connection to create the PreparedStatement with
     * @param parsedSql         the parsed representation of the SQL statement
     * @param parameters        the source for named parameters
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
     * @param conn                 the connection to create the PreparedStatement with
     * @param parsedSql            the parsed representation of the SQL statement
     * @param parameters           the source for named parameters
     * @param resultSetType        - one of the following ResultSet constants: ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
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
     * @param conn         the connection to create the PreparedStatement with
     * @param sqlStatement a SQL statement
     * @param parameters   the source for named parameters
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
     * @param ps         the PreparedStatement which named parameters must be replaced with JDBC placeholders('?')
     * @param parsedSql  the parsed sql which named parameters must be replaced
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
     * @param ps            the PreparedStatement which named parameter must be replaced with a JDBC placeholder('?')
     * @param indexPosition the index where the named parameter is defined in the PreparedStatement
     * @param parameter     the parameter value that must replace the named parameter
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
     * Convert the given array to a list recursively ie each array contained in the given array are also converted to a list.
     *
     * @param array the array to convert into a list
     * @return the converted list
     * @throws IllegalArgumentException if array is not an actual array
     */
    public static List toList( Object array ) {

        if ( !OliveUtils.isArray( array ) ) {
            throw new IllegalArgumentException( "object must be an array" );
        }

        List list = new ArrayList();

        int length = java.lang.reflect.Array.getLength( array );

        for ( int i = 0; i < length; i++ ) {

            Object arrayItem = java.lang.reflect.Array.get( array, i );

            if ( OliveUtils.isObjectArray( arrayItem ) ) {
                list.add( ( Object[] ) arrayItem );

            } else if ( OliveUtils.isPrimitiveArray( arrayItem ) ) {

                List innerList = toSimpleList( arrayItem );
                list.add( innerList );

            } else {
                list.add( arrayItem );

            }
        }

        return list;
    }

    public static RuntimeException closeQuietly( boolean autoCommit, Exception exception, AutoCloseable... closeables ) {

        List list = Arrays.asList( closeables );
        return closeQuietly( autoCommit, exception, list );
    }

    public static RuntimeException closeQuietly( boolean autoCommit, Exception exception, Iterable<? extends AutoCloseable> closeables ) {

        try {
            close( autoCommit, closeables );
            return OliveUtils.toRuntimeException( exception );

        } catch ( RuntimeException ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
            return OliveUtils.toRuntimeException( exception );
        }
    }

    public static RuntimeException closeQuietly( Exception exception, Iterable<? extends AutoCloseable> closeables ) {

        try {
            OliveUtils.close( closeables );

        } catch ( RuntimeException ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
        }

        return OliveUtils.toRuntimeException( exception );
    }

    public static RuntimeException closeQuietly( boolean autoCommit, Iterable<? extends AutoCloseable> closeables ) {
        return closeQuietly( autoCommit, null, closeables );
    }

    public static RuntimeException closeQuietly( boolean autoCommit, AutoCloseable... closeables ) {
        return closeQuietly( autoCommit, null, closeables );
    }

    /**
     * Closes the given list of autoCloseabes and wraps any Exceptions thrown as RuntimeExcepions. Every closeable will have its #close method called, regardless
     * if an exception is thrown. Any connections will have their setCommit status set to the given value.
     * <p/>
     * <b>Note:</b> exceptions thrown by the autoClosable objects will be chained using Throwable#addSuppressed(Throwable).
     * <p/>
     * <p>
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
                Connection conn = ( Connection ) closeable;

                try {
                    conn.setAutoCommit( autoCommit );

                } catch ( SQLException ex ) {
                    mainException = OliveUtils.addSuppressed( ex, mainException );
                }

            }

            try {
                closeable.close();
            } catch ( final Exception ex ) {
                mainException = OliveUtils.addSuppressed( ex, mainException );
            }
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
    }

    /**
     * Closes the given autoCloseabe array and wraps any Exceptions thrown as RuntimeExcepions. Every closeable will have its #close method called, regardless
     * if an exception is thrown.
     * <p/>
     * <b>Note:</b> exceptions thrown by the autoClosable objects will be chained using Throwable#addSuppressed(Throwable).
     * <p/>
     * <p>
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
                    return ( Connection ) closeable;
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
                    connections.add( ( Connection ) closeable );
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

        if ( !OliveUtils.isArray( obj ) ) {
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

    public static void main( String[] args ) {
    }

    public static <T> T mapToBean( PreparedStatement ps, RowMapper<T> mapper ) {
        //StatementContainer stContainer = getStatementContainer( conn, ps );

        ResultSet rs = null;

        Exception exception = null;

        try {
            rs = ps.executeQuery();

            int rowNum = 0;

            while ( rs.next() ) {

                // Dont use rs.getRow, scrollable ResultSet curosrs not supported by Derby
                //int rowNum = rs.getRow();
                T t = mapper.map( rs, rowNum++ );
                return t;
            }

        } catch ( Exception ex ) {
            exception = ex;
            throw new RuntimeException( ex );

        } finally {
            exception = OliveUtils.closeQuietly( exception, rs );
            OliveUtils.throwAsRuntimeIfException( exception );
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

        Exception exception = null;

        try {

            rs = ps.executeQuery();

            int rowNum = 0;

            while ( rs.next() ) {

                // Dont use rs.getRow, scrollable ResultSet curosrs not supported by Derby
                //int rowNum = rs.getRow();
                T t = mapper.map( rs, rowNum++ );
                list.add( t );
            }

        } catch ( SQLException ex ) {
            exception = ex;
            throw new RuntimeException( ex );

        } finally {
            exception = OliveUtils.closeQuietly( exception, rs );
            OliveUtils.throwAsRuntimeIfException( exception );
        }

        return list;
    }

    public static <T> List<T> mapToBeans( PreparedStatement ps, RowMapper<T> mapper ) {
        return mapToList( ps, mapper );
    }

    public static <T> T mapToPrimitive( Class<T> cls, PreparedStatement ps ) {

        ResultSet rs = null;

        Exception exception = null;

        try {

            rs = ps.executeQuery();
            if ( rs.next() ) {

                if ( cls == int.class || cls == Integer.class ) {
                    Integer value = rs.getInt( 1 );
                    return ( T ) value;

                } else if ( cls == long.class || cls == Long.class ) {
                    Long value = rs.getLong( 1 );
                    return ( T ) value;

                } else if ( cls == byte.class || cls == Byte.class ) {
                    Byte value = rs.getByte( 1 );
                    return ( T ) value;

                } else if ( cls == boolean.class || cls == Boolean.class ) {
                    Boolean value = rs.getBoolean( 1 );
                    return ( T ) value;

                } else if ( cls == double.class || cls == Double.class ) {
                    Double value = rs.getDouble( 1 );
                    return ( T ) value;

                } else if ( cls == float.class || cls == Float.class ) {
                    Float value = rs.getFloat( 1 );
                    return ( T ) value;

                } else if ( cls == short.class || cls == Short.class ) {
                    Short value = rs.getShort( 1 );
                    return ( T ) value;

                } else if ( cls == String.class ) {
                    String value = rs.getString( 1 );
                    return ( T ) value;

                } else if ( cls == java.util.Date.class ) {
                    java.util.Date value = rs.getTimestamp( 1 );
                    return ( T ) value;

                } else if ( cls == BigDecimal.class ) {
                    BigDecimal value = rs.getBigDecimal( 1 );
                    return ( T ) value;

                } else {
                    throw new IllegalArgumentException( cls + " is not a supported type" );

                }
            }

        } catch ( SQLException ex ) {
            exception = ex;
            throw new RuntimeException( exception );

        } finally {
            exception = OliveUtils.closeQuietly( exception, rs );
            OliveUtils.throwAsRuntimeIfException( exception );

        }

        return null;
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

    public static SQLException convertSqlExceptionToSuppressed( SQLException e ) {
        for ( SQLException next = e.getNextException(); next != null; next = next.getNextException() ) {
            e.addSuppressed( next );
        }
        return e;
    }

    public static void setReadOnly( Connection conn, boolean value ) {
        try {
            conn.setReadOnly( value );
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static boolean isClosed( ResultSet rs ) {
        try {
            return rs.isClosed();
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static boolean isClosed( Statement stmt ) {
        try {
            return stmt.isClosed();
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static boolean isClosed( Connection conn ) {
        try {
            return conn.isClosed();
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }
}
