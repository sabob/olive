package za.sabob.olive.jdbc.context;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import za.sabob.olive.jdbc.context.listener.*;
import za.sabob.olive.util.*;
import static za.sabob.olive.util.OliveUtils.toRuntimeException;

public class JDBCContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger( JDBCContext.class.getName() );

    private Connection connection;

    private JDBCContext parent;

    private JDBCContext child;

    private boolean closed;

    private boolean autoCommitValueRetrievedFromDataSource = true;

    private boolean transaction = false;

    private JDBCContextListener listener; // TODO listener or listeners

    private final List<Statement> statements = new ArrayList<>();

    private final List<ResultSet> resultSets = new ArrayList<>();

    public JDBCContext( Connection conn ) {
        this.connection = conn;
    }

    public JDBCContext( Connection conn, boolean transaction ) {
        this.connection = conn;
        this.transaction = transaction;
    }

    public JDBCContext( Connection conn, JDBCContextListener listener, boolean transaction ) {
        this.connection = conn;
        this.listener = listener;
        this.transaction = transaction;
    }

    public JDBCContext( Connection conn, JDBCContextListener listener, boolean transaction, boolean autoCommitValueRetrievedFromDataSource ) {
        this( conn, listener, transaction );
        this.autoCommitValueRetrievedFromDataSource = autoCommitValueRetrievedFromDataSource;
    }

    public boolean getAutoCommitValueRetrievedFromDataSource() {
        return autoCommitValueRetrievedFromDataSource;
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList( statements );
    }

    public List<ResultSet> getResultSets() {
        return Collections.unmodifiableList( resultSets );
    }

    public boolean isRootTransactionContext() {

        if ( isClosed() ) {
            return false;
        }

        JDBCContext parent = getParent();

        while ( parent != null ) {
            boolean isParentRootTransactionContext = parent.transaction;
            if ( isParentRootTransactionContext ) {
                return false;
            }
            parent = parent.getParent();
        }
        return transaction;
    }

    public boolean isRootContext() {
        boolean isRoot = getParent() == null;
        return isRoot;
    }

    public void commit() {

        if ( canCommit() ) {
            Connection conn = getConnection();
            OliveUtils.commit( conn );

        }
    }

    public RuntimeException commitQuietly() {
        Connection conn = getConnection();
        return OliveUtils.commitQuietly( conn );
    }

    public void rollback() {
        if ( canRollback() ) {
            Connection conn = getConnection();
            OliveUtils.rollback( conn );
        }
    }

    public RuntimeException rollbackQuietly() {
        if ( canRollback() ) {
            Connection conn = getConnection();
            return OliveUtils.rollbackQuietly( conn );
        }
        return null;
    }

    public RuntimeException rollback( Exception e ) {
        if ( e == null ) {
            throw new IllegalArgumentException( "exception cannot be null as rollback is expected to return a RuntimeException which wraps the given exception." );
        }

        if ( canRollback() ) {
            Connection conn = getConnection();
            return OliveUtils.rollback( conn, e );
        }
        return toRuntimeException( e );
    }

    public RuntimeException rollbackQuietly( Exception e ) {
        return rollback( e );
    }

    public boolean canRollback() {
        return isRootTransactionContext();
    }

    public boolean canCommit() {
        return isRootTransactionContext();
    }

    public boolean canCloseConnection() {
        return isRootConnectionHolder();
    }

    public boolean isRootConnectionHolder() {
        if ( isClosed() ) {
            return false;
        }
        return isRootContext();
//
//        Connection conn = getConnection();
//        JDBCContext parent = getParent();
//
//        while ( parent != null ) {
//            Connection parentConn = parent.getConnection();
//            if ( parentConn == conn ) {
//                return false;
//            }
//            parent = parent.getParent();
//        }
//        return true;
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
                "You are retrieving a Connection from a JDBCContext that is closed. Either you closed JDBCContext already or you forgot to begin an operation through JDBC.beginOperation or TX.beginTransaction." );
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

        closeChildren();

        if ( canCloseConnection() ) {
            closeIncludingConnection();

        } else {
            closeExcludingConnection();

        }

        // TODO should we nullify connection here to prevent leaks?
        //connection = null;
        setClosed( true );

        detach();

    }

    protected void closeChildren() {

        if ( child == null ) {
            return;
        }

        child.close();
    }

    private void closeIncludingConnection() {

        List<AutoCloseable> closeables = gatherResources();
        boolean autoCommit = autoCommitValueRetrievedFromDataSource;
        RuntimeException exception = OliveUtils.closeQuietly( autoCommit, closeables );

        fireConnectionClosed();

        OliveUtils.throwAsRuntimeIfException( exception );
    }

    private void closeExcludingConnection() {
        List<AutoCloseable> closeables = gatherResources();

        // Dont close the connection since we aren't referencing the root connection
        closeables.remove( connection );

        RuntimeException exception = OliveUtils.closeQuietly( closeables );

        if ( isRootTransactionContext() ) {
            // We are leaving the root TX context (but not the root context), so switch connection back to autoCommit
            OliveUtils.setAutoCommit( connection, true );
        }

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

    public JDBCContext getParent() {
        return parent;
    }

    public void setParent( JDBCContext parent ) {
        this.parent = parent;
    }

    public boolean hasChild() {
        return child != null;
    }

    public JDBCContext getChild() {
        return child;
    }

    public void setChild( JDBCContext childArg ) {

        if ( childArg == null ) {
            this.child = null;
            return;
        }

        if ( !hasChild() ) {
            this.child = childArg;
            return;
        }

        throw new IllegalStateException( "Cannot override JDBCContext child with another child!" );
    }

    public JDBCContext getRootContext() {
        if ( isRootContext() ) {
            return this;
        }

        return getParent().getRootContext();
    }

    public JDBCContext getMostRecentContext() {

        if ( !hasChild() ) {
            return this;
        }

        return getChild().getMostRecentContext();
    }

    public void attach( JDBCContext child ) {

        if ( child == null ) {
            throw new IllegalArgumentException( "Child JDBCContext cannot be null." );
        }

        if ( child == this ) {
            throw new IllegalArgumentException( "Cannot attach JDBCContext to itself." );
        }

        if ( child.getParent() != null ) {
            throw new IllegalStateException(
                "Cannot attach child to this JDBCContext because child is still attached to another JDCContext. First detach the child." );
        }

        if ( hasChild() && !getChild().isClosed() ) {
            throw new IllegalStateException(
                "Cannot attach child because this JDBCContext' current child is not closed yet. Close the current child before attaching a new child." );
        }

        child.setParent( this );
        setChild( child );
    }

    public void detach() {
        if ( hasListener() ) {
            getListener().onBeforeContextDetach( this );
        }

        if ( isOpen() ) {
            throw new IllegalStateException( "You have to close the JDBCContext before detaching." );
        }

        JDBCContext parentCtx = getParent();

        if ( parentCtx != null ) {
            parentCtx.setChild( null );
        }

        setParent( null );
    }

    public JDBCContextListener getListener() {
        return listener;
    }

    public void setListener( JDBCContextListener listener ) {
        this.listener = listener;
    }

    public boolean hasListener() {
        return getListener() != null;
    }

    private void fireConnectionClosed() {
        if ( hasListener() ) {
            getListener().onConnectionClosed( this );
        }
    }
}
