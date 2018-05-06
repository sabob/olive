package za.sabob.olive.jdbc.context;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.listener.*;
import za.sabob.olive.util.*;

public class JDBCContextManager {

    private Connection conn;

    private Connection txConn;

    private JDBCContext rootCtx;

    private final JDBCContextListener contextListener = new ManagerListener();

    public JDBCContext createContext( DataSource ds, boolean tx ) {
        JDBCContext ctx = createJDBCContext( ds, tx );
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

    public JDBCContext createJDBCContext( DataSource ds, boolean tx ) {

        Connection mostRecentConn = getConnection( tx );

        if ( mostRecentConn == null ) {
            boolean autoCommit = !tx;
            mostRecentConn = getNewConnection( ds, autoCommit );
        }

        JDBCContext ctx = new JDBCContext( mostRecentConn, contextListener );

        if ( rootCtx == null ) {
            rootCtx = ctx;

        } else {
            JDBCContext mostRecentCtx = getMostRecentContext();
            attach( mostRecentCtx, ctx );
        }

        updateConnection( ctx, tx );

        return ctx;
    }

    public Connection getNewConnection( DataSource ds, boolean autoCommit ) {

        Connection newConn = null;

        try {
            newConn = OliveUtils.getConnection( ds, true );
            OliveUtils.setAutoCommit( newConn, autoCommit );
            return newConn;

        } catch ( Exception e ) {
            throw OliveUtils.closeSilently( true, e, newConn );
        }
    }

    public void updateConnection( JDBCContext ctx, boolean tx ) {
        if ( tx ) {
            txConn = ctx.getConnection();

        } else {
            conn = ctx.getConnection();
        }

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

    protected Connection getConnection( boolean tx ) {

        if ( tx ) {
            return txConn;

        } else {
            return conn;
        }
    }

    public boolean isEmpty() {
        return rootCtx == null;
    }

    protected void resetConnection( Connection connToReset ) {

        if ( connToReset == conn ) {
            conn = null;

        } else if ( connToReset == txConn ) {
            txConn = null;
        }
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
                Connection contextConn = ctx.getConnection();
                resetConnection( contextConn );
            }

        }

    }

    private void removeManager( JDBCContext ctx, boolean isRootContext ) {
        if ( DSF.hasDataSourceContainer() ) {
            DataSourceContainer container = DSF.getDataSourceContainer();
            container.removeManager( ctx, isRootContext );

        }
    }

}
