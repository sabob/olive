package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import java.util.logging.*;

import za.sabob.olive.config.OliveConfig;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.util.*;

import javax.sql.DataSource;


public class JDBCContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger( JDBCContext.class.getName() );

    private DataSource dataSource;

    private Connection connection;

    private boolean closed;

    private boolean transaction = false;

    private final List<Statement> statements = new ArrayList<>();

    private final List<ResultSet> resultSets = new ArrayList<>();

    public JDBCContext() {
        this( OliveConfig.getDefaultDataSource() );
    }

    public JDBCContext( boolean beginTransaction ) {
        this( OliveConfig.getDefaultDataSource(), beginTransaction );
    }

    public JDBCContext( Connection conn ) {
        this.connection = conn;
        this.transaction = false;
    }

    public JDBCContext( Connection conn, boolean beginTransaction ) {
        this.connection = conn;
        this.transaction = beginTransaction;
        boolean autoCommit = !beginTransaction;
        JDBCUtils.setAutoCommit( conn, autoCommit );
    }

    public JDBCContext( DataSource ds ) {
        this( ds, false );
    }

    public JDBCContext( DataSource ds, boolean beginTransaction ) {
        this.connection = JDBCUtils.getConnection( ds, beginTransaction );
        this.dataSource = ds;
        this.transaction = beginTransaction;

        if ( beginTransaction ) {
            JDBC.transactionStarted( ds );
        } else {
            JDBC.operationStarted( ds );
        }
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList( statements );
    }

    public List<ResultSet> getResultSets() {
        return Collections.unmodifiableList( resultSets );
    }

    public void commit() {

        Connection conn = getConnection();
        JDBCUtils.commit( conn );


    }

    public RuntimeException commitQuietly() {
        Connection conn = getConnection();
        return JDBCUtils.commitQuietly( conn );
    }

    public void rollback() {
        Connection conn = getConnection();
        JDBCUtils.rollback( conn );
    }

    public RuntimeException rollbackQuietly() {
        Connection conn = getConnection();
        return JDBCUtils.rollbackQuietly( conn );
    }

    public RuntimeException rollbackQuietly( Exception ex ) {
        if ( ex == null ) {
            throw new IllegalArgumentException( "exception cannot be null as rollback is expected to return a RuntimeException which wraps the given exception." );
        }

        Connection conn = getConnection();
        return JDBCUtils.rollbackQuietly( conn, ex );
    }

    public void rollbackAndThrow( Exception ex ) {
        RuntimeException exception = rollbackQuietly( ex );
        OliveUtils.throwAsRuntimeIfException( exception );
    }

    public boolean hasConnection() {
        return connection != null;
    }

    /**
     * Used for testing purposes.
     *
     * @return true if the connection is closed, false otherwise
     */
    public boolean isConnectionClosed() {
        if ( connection == null ) {
            return true;
        }
        return JDBCUtils.isClosed( connection );
    }

    public Connection getConnection() {

        if ( isClosed() ) {
            Throwable t = new Throwable(
                    "You are retrieving a Connection from a JDBCContext that is closed. Either you closed JDBCContext already or you forgot to begin an operation through JDBC.beginOperation or JDBC.beginTransaction." );
            LOGGER.log( Level.FINE, t.getMessage(), t );
        }

        return connection;
    }

    public void add( Statement statement ) {

        if ( statements.contains( statement ) ) {
            return;
        }

        this.statements.add( statement );
    }

    public void add( ResultSet rs ) {

        if ( resultSets.contains( rs ) ) {
            return;
        }

        this.resultSets.add( rs );
    }

    private void clear() {
        this.resultSets.clear();
        this.statements.clear();
        this.connection = null;
        this.dataSource = null;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public boolean isClosed() {
        return this.closed;
    }

    private void setClosed( boolean closed ) {
        this.closed = closed;
    }

    public RuntimeException closeQuietly() {

        try {
            close();
            return null;

        } catch ( Exception e ) {
            return OliveUtils.toRuntimeException( e );
        }
    }

    public void closeAndThrow( Exception ex ) {
        RuntimeException exception = closeQuietly( ex );
        OliveUtils.throwAsRuntimeIfException( exception );
    }

    public RuntimeException closeQuietly( Exception exception ) {
        RuntimeException ex = closeQuietly();
        exception = OliveUtils.addSuppressed( ex, exception );
        return OliveUtils.toRuntimeException( exception );
    }

    @Override
    public void close() {

        if ( isClosed() ) {
            return;
        }

        closeIncludingConnection();
        // TODO should we nullify connection here to prevent leaks?
        this.connection = null;
        setClosed( true );

        if ( this.transaction ) {
            JDBC.transactionFinished( this.dataSource );

        } else {
            JDBC.operationFinished( this.dataSource );

        }
        this.dataSource = null;
    }

    private void closeIncludingConnection() {

        List<AutoCloseable> closeables = gatherResources();
        boolean autoCommit = true;
        RuntimeException exception = JDBCUtils.closeQuietly( autoCommit, closeables );

        OliveUtils.throwAsRuntimeIfException( exception );
    }

    public void closeExcludingConnection() {
        List<AutoCloseable> closeables = gatherResources();

        // Dont close the connection
        closeables.remove( connection );

        RuntimeException exception = OliveUtils.closeQuietly( closeables );

        boolean autoCommit = true;
        JDBCUtils.setAutoCommit( connection, autoCommit );

        OliveUtils.throwAsRuntimeIfException( exception );
    }

    public List<AutoCloseable> gatherResources() {

        List<AutoCloseable> resources = new ArrayList();

        List<ResultSet> resultSets = getResultSets();
        resources.addAll( resultSets );

        List<Statement> statements = getStatements();
        resources.addAll( statements );

        resources.add( connection );

        return resources;
    }
}
