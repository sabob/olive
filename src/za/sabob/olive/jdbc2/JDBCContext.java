package za.sabob.olive.jdbc2;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import za.sabob.olive.jdbc2.listener.*;
import za.sabob.olive.util.*;
import static za.sabob.olive.util.OliveUtils.toRuntimeException;

public class JDBCContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger( JDBCContext.class.getName() );

    private Connection connection;

    private JDBCContext parent;

    private JDBCContext child;

    private boolean closed;

    protected Boolean contextHolder = null; // TODO review this now we have listener

    private Boolean canCloseConnection; // TODO review this now we have listener

    private JDBCContextListener listener; // TODO listener or listeners

    private final List<Statement> statements = new ArrayList<>();

    private final List<ResultSet> resultSets = new ArrayList<>();

    public JDBCContext() {
    }

    public JDBCContext( JDBCContextListener listener ) {
        this.listener = listener;
    }

    public JDBCContext( boolean contextHolder ) {
        this.contextHolder = contextHolder;
    }

    public JDBCContext( Connection conn ) {
        this.connection = conn;
    }

    public JDBCContext( Connection conn, JDBCContextListener listener ) {
        this.connection = conn;
        this.listener = listener;
    }

    public JDBCContext( Connection conn, boolean canCloseConnectionArg ) {
        this.connection = conn;
        this.canCloseConnection = canCloseConnectionArg;
    }

    public JDBCContext( Connection conn, boolean canCloseConnection, JDBCContextListener listener ) {
        this.connection = conn;
        this.canCloseConnection = canCloseConnection;
        this.listener = listener;
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList( statements );
    }

    public List<ResultSet> getResultSets() {
        return Collections.unmodifiableList( resultSets );
    }

    public boolean isRootContext() {

        JDBCContext parentCtx = getParent();
        if ( parentCtx == null || parentCtx.isContextHolder() ) {
            return true;
        }

        return false;
    }

    public void commit() {

        if ( canCommit() ) {
            Connection conn = getConnection();
            OliveUtils.commit( conn );

        }
    }

    public RuntimeException commitSilently() {
        Connection conn = getConnection();
        return OliveUtils.commitSilently( conn );
    }

    public void rollback() {
        if ( canRollback() ) {
            Connection conn = getConnection();
            OliveUtils.rollback( conn );
        }
    }

    public RuntimeException rollbackSilently() {
        if ( canRollback() ) {
            Connection conn = getConnection();
            return OliveUtils.rollbackSilently( conn );
        }
        return null;
    }

    public RuntimeException rollback( Exception e ) {
        if (e == null) {
            throw new IllegalArgumentException("exception cannot be null as rollback is expected to return a RuntimeException which wraps the given exception.");
        }
        
        if ( canRollback() ) {
            Connection conn = getConnection();
            return OliveUtils.rollback( conn, e );
        }
        return toRuntimeException( e );
    }

    public RuntimeException rollbackSilently( Exception e ) {
        if (e == null) {
            throw new IllegalArgumentException("exception cannot be null as rollback is expected to return a RuntimeException which wraps the given exception.");
        }
        
        if ( canRollback() ) {
            Connection conn = getConnection();
            return OliveUtils.rollback( conn, e );
        }
        return toRuntimeException( e );
    }

    public boolean canRollback() {
        return canCloseConnection();
    }

    public boolean canCommit() {
        return canCloseConnection();
    }

    public boolean canCloseConnection() {

        if ( canCloseConnection == null ) {

            if ( isRootContext() ) {
                return true;

            } else {
                return false;
            }
        }

        return canCloseConnection;
    }

    public boolean hasConnection() {
        return connection != null;
    }

    public Connection getConnection() {

        if ( isClosed() ) {
            LOGGER.info(
                "You are retrieving a Connection that is closed. Either you closed JDBCContext already or you forgot to begin an operation through JDBC.beginOperation or TX.beginTransaction." );
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

    public void clear() {
        this.resultSets.clear();
        this.statements.clear();
        this.connection = null;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed( boolean closed ) {
        this.closed = closed;
    }

    @Override
    public void close() {

        if ( isClosed() ) {
            return;
        }

        closeChildren();

        //if ( isAutoCloseConnection() ) {
        if ( canCloseConnection() ) {
            closeIncludingConnection();

        } else {
            closeExcludingConnection();

        }

        // TODO should we nullify connection here to prevent leaks?
        //connection = null;
        closed = true;

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
        boolean autoCommit = true;
        RuntimeException exception = OliveUtils.closeSilently( autoCommit, closeables );

        fireConnectionClosed();

        OliveUtils.throwAsRuntimeIfException( exception );
    }

    private void closeExcludingConnection() {
        List<AutoCloseable> closeables = gatherResources();

        // Dont close the connection since we aren't referencing the root connection
        closeables.remove( connection );

        boolean autoCommit = true;
        RuntimeException exception = OliveUtils.closeSilently( autoCommit, closeables );

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
        JDBCContext parentCtx = getParent();
        if ( parent == null || parentCtx.isContextHolder() ) {
            return this;
        }

        return parent.getRootContext();
    }

    public JDBCContext getMostRecentContext() {

        if ( !hasChild() ) {
            return this;
        }

        return getChild().getMostRecentContext();
    }

    public void attach( JDBCContext child ) {
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

        if ( isOpen() && !isContextHolder() ) {
            throw new IllegalStateException( "You have to close the JDBCContext before detaching." );
        }

        JDBCContext parentCtx = getParent();

        //connection = null;
        if ( parentCtx != null ) {
            parentCtx.setChild( null );
        }

        setParent( null );
    }

    public boolean isContextHolder() {
        if ( contextHolder == null ) {
            return false;
        }

        return contextHolder;
    }

    public void setContextHolder( boolean contextHolderArg ) {
        if ( contextHolder == null ) {
            contextHolder = contextHolderArg;

        } else {
            throw new IllegalStateException( "ContextHolder is already set to " + contextHolder + ". Cannot change it." );
        }

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
