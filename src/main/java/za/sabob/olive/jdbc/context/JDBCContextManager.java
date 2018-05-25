package za.sabob.olive.jdbc.context;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.config.*;
import za.sabob.olive.jdbc.context.listener.*;
import za.sabob.olive.util.*;

public class JDBCContextManager {

    private Connection conn;

    private JDBCContext rootCtx;

    private final JDBCContextListener contextListener = new ManagerListener();

    public JDBCContext createContext( DataSource ds, boolean beginTransaction ) {
        JDBCContext ctx = createJDBCContext( ds, beginTransaction );
        return ctx;
    }

    public JDBCContext getRootContext() {
        return rootCtx;
    }

    public JDBCContext getMostRecentContext() {

        if ( rootCtx == null ) {
            return null;
        }

        return rootCtx.getMostRecentContext();
    }

    public JDBCContext createJDBCContext( DataSource ds, boolean beginTransaction ) {

        Connection contextConn = getConnection();
        boolean autoCommitValueRetrievedFromDataSource = true;

        JDBCContext ctx = null;

        boolean autoCommit = !beginTransaction;

        if ( contextConn == null ) {

            ConnectionAutoAndCommitValue connectionAndAutoCommitValue = getNewConnection( ds, autoCommit );

            contextConn = connectionAndAutoCommitValue.conn;
            autoCommitValueRetrievedFromDataSource = connectionAndAutoCommitValue.autoCommit;

            ctx = new JDBCContext( contextConn, contextListener, beginTransaction, autoCommitValueRetrievedFromDataSource );

        } else {

            boolean autoCommitOfCurrentConnection = getAutoCommitOrClose( contextConn );
            ctx = new JDBCContext( contextConn, contextListener, beginTransaction );

            boolean isTransactionRunning = !autoCommitOfCurrentConnection;

            if ( beginTransaction && isTransactionRunning ) {
                if ( !JDBCConfig.isJoinableTransactions() ) {
                    throw new IllegalStateException(
                        "You are not allowed to start nested transactions for this DataSource. This DataSource connection is already busy with a transaction."
                        + " To allow transactions to join set JDBCConfig.setJoinableTransactions( true )" );
                }
            }

            if ( beginTransaction && !isTransactionRunning ) {
                // switch on transaction
                setAutoCommitOrClose( ctx, autoCommit, getRootContext().getAutoCommitValueRetrievedFromDataSource() );

            }
        }

        if ( rootCtx == null ) {
            rootCtx = ctx;

        } else {
            JDBCContext mostRecentCtx = getMostRecentContext();
            attach( mostRecentCtx, ctx );
        }

        updateConnection( ctx );

        return ctx;
    }

    private ConnectionAutoAndCommitValue getNewConnection( DataSource ds, boolean autoCommit ) {

        Connection newConn = null;
        boolean currentAutoCommit = true;

        try {
            newConn = OliveUtils.getConnection( ds );
            currentAutoCommit = newConn.getAutoCommit();
            OliveUtils.setAutoCommit( newConn, autoCommit );

            ConnectionAutoAndCommitValue result = new ConnectionAutoAndCommitValue( newConn, currentAutoCommit );

            return result;

        } catch ( Exception e ) {
            RuntimeException re = OliveUtils.closeQuietly( currentAutoCommit, e, newConn );
            throw re;
        }

    }

    public void updateConnection( JDBCContext ctx ) {
        conn = ctx.getConnection();
    }

    public void attach( JDBCContext parent, JDBCContext child ) {

        if ( parent == null || child == null || parent == child ) {
            return;
        }

        parent.attach( child );
    }

    public void detach( JDBCContext ctx ) {
        ctx.detach();

    }

    protected Connection getConnection() {
        return conn;
    }

    public boolean isEmpty() {
        return rootCtx == null;
    }

    protected void resetConnection() {
        conn = null;
    }

    private class ManagerListener extends JDBCContextListener {

        @Override
        public void onBeforeContextDetach( JDBCContext ctx ) {

            boolean isRootContext = ctx == rootCtx;
            // Sanity check
            if ( isRootContext && !ctx.isRootContext() ) {
                throw new IllegalStateException( "JDBCContext is not the root context, but JDBCContextManager states that it is!" );
            }

            if ( isRootContext ) {
                rootCtx = null;
            }

            removeManager( ctx, isRootContext );
        }

        @Override
        public void onConnectionClosed( JDBCContext ctx ) {

            if ( ctx.hasConnection() ) {
                resetConnection();
            }

        }

    }

    private void removeManager( JDBCContext ctx, boolean isRootContext ) {
        if ( DSF.hasDataSourceContainer() ) {
            DataSourceContainer container = DSF.getDataSourceContainer();
            container.removeManager( ctx, isRootContext );

        }
    }

    private class ConnectionAutoAndCommitValue {

        private boolean autoCommit = true;

        private Connection conn = null;

        public ConnectionAutoAndCommitValue( Connection conn, boolean autoCommit ) {
            this.conn = conn;
            this.autoCommit = autoCommit;

        }

    }

    private boolean getAutoCommitOrClose( Connection connArg ) {
        try {
            return connArg.getAutoCommit();

        } catch ( SQLException ex ) {
            RuntimeException re = OliveUtils.closeQuietly( connArg );
            throw re;
        }
    }

    private void setAutoCommitOrClose( JDBCContext ctx, boolean autoCommit, boolean originalAutoCommit ) {

        try {
            OliveUtils.setAutoCommit( ctx.getConnection(), autoCommit );

        } catch ( Exception e ) {

            if ( ctx.isRootConnectionHolder() ) {
                RuntimeException re = OliveUtils.closeQuietly( originalAutoCommit, e, ctx.getConnection() );
                throw re;
            }

            throw OliveUtils.toRuntimeException( e );
        }

    }

}
