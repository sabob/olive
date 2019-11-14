package za.sabob.olive.jdbc.context;

import java.sql.*;
import java.util.*;
import java.util.logging.*;

import za.sabob.olive.util.*;

import static za.sabob.olive.util.OliveUtils.toRuntimeException;

public class JDBCContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger( JDBCContext.class.getName() );

    private Connection connection;

    private boolean closed;

    private boolean transaction = false;

    private final List<Statement> statements = new ArrayList<>();

    private final List<ResultSet> resultSets = new ArrayList<>();

    public JDBCContext( Connection conn ) {
        this.connection = conn;
    }

    public JDBCContext( Connection conn, boolean transaction ) {
        this.connection = conn;
        this.transaction = transaction;
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList( statements );
    }

    public List<ResultSet> getResultSets() {
        return Collections.unmodifiableList( resultSets );
    }

    public void commit() {

        Connection conn = getConnection();
        OliveUtils.commit( conn );


    }

    public RuntimeException commitQuietly() {
        Connection conn = getConnection();
        return OliveUtils.commitQuietly( conn );
    }

    public void rollback() {
        Connection conn = getConnection();
        OliveUtils.rollback( conn );
    }

    public RuntimeException rollbackQuietly() {
        Connection conn = getConnection();
        return OliveUtils.rollbackQuietly( conn );
    }

    public RuntimeException rollback( Exception ex ) {
        if ( ex == null ) {
            throw new IllegalArgumentException( "exception cannot be null as rollback is expected to return a RuntimeException which wraps the given exception." );
        }

        Connection conn = getConnection();
        return OliveUtils.rollback( conn, ex );
    }

    public RuntimeException rollbackQuietly( Exception e ) {
        return rollback( e );
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

        try {
            return connection.isClosed();
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
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

    @Override
    public void close() {

        if ( isClosed() ) {
            return;
        }

        closeIncludingConnection();
        // TODO should we nullify connection here to prevent leaks?
        connection = null;
        setClosed( true );
    }

    private void closeIncludingConnection() {

        List<AutoCloseable> closeables = gatherResources();
        boolean autoCommit = true;
        RuntimeException exception = OliveUtils.closeQuietly( autoCommit, closeables );

        OliveUtils.throwAsRuntimeIfException( exception );
    }

    public void closeExcludingConnection() {
        List<AutoCloseable> closeables = gatherResources();

        // Dont close the connection since we aren't referencing the root connection
        closeables.remove( connection );

        RuntimeException exception = OliveUtils.closeQuietly( closeables );

        boolean autoCommit = true;
        OliveUtils.setAutoCommit( connection, autoCommit );

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
