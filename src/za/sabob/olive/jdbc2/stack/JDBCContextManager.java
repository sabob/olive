package za.sabob.olive.jdbc2.stack;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.listener.*;
import za.sabob.olive.util.*;

public class JDBCContextManager {

    //private JDBCContext mostRecentCtx;
    //private JDBCContext mostRecentNonTxCtx;
    //private JDBCContext mostRecentTxCtx;
    //private JDBCContext ctxHolder = new JDBCContext( true );
    //private JDBCContext ctxHolder = new JDBCContext( true );// Can remove this with listener?

    private Connection conn;

    private Connection txConn;
    
    private JDBCContext rootCtx;

    private JDBCContextListener contextListener = new ManagerListener();

//    private JDBCContextStack stack = new JDBCContextStack();
//
//    private JDBCContextStack txStack = new JDBCContextStack();
//
//
//    protected JDBCContextStack getStack() {
//        return stack;
//    }
//
//    protected JDBCContextStack getTxStack() {
//        return txStack;
//    }
    public JDBCContext createContext( DataSource ds, boolean tx ) {
        JDBCContext ctx = createJDBCContext( ds, tx );
        return ctx;
    }
    
    public JDBCContext getRootContext() {
        return rootCtx;
    }

    public JDBCContext getMostRecentContext() {
        
        if (rootCtx == null) return null;
        
        return rootCtx.getMostRecentContext();
    }

//    public JDBCContext getMostRecentTxContext() {
//        return mostRecentTxCtx;
//    }
//
//    public JDBCContext getMostRecentNonTxContext() {
//        return mostRecentNonTxCtx;
//    }
//    public JDBCContext peekTop( boolean tx ) {
//        // TODO should we find JDBCContext or the underlying Connection?
//        // Either way a NEW JDBCContext must be returned, even if it closed the old one
//        // BUT where should JDBCContext be closed?? Only in root or every try {} call down the chain? PRobably every call down the chain? 
//
//        if ( tx ) {
//            return getTxStack().peekTop();
//        } else {
//            return getStack().peekTop();
//        }
//    }
//    public void add( JDBCContext ctx, boolean tx ) {
//        if ( tx ) {
//            getTxStack().add( ctx );
//        }
//        getStack().add( ctx );
//    }
    public JDBCContext createJDBCContext( DataSource ds, boolean tx ) {

        Connection mostRecentConn = getConnection( tx );

        boolean canCloseConnection = false;
        //boolean isRootContext = false;

        if ( mostRecentConn == null ) {
            boolean autoCommit = !tx;
            mostRecentConn = getNewConnection( ds, autoCommit );
            canCloseConnection = true;
            //isRootContext = true;
        }

        JDBCContext ctx = new JDBCContext( mostRecentConn, canCloseConnection, contextListener );

//        if ( isRootContext ) {
//            ctxHolder.attach( ctx );
//        }

        if (rootCtx == null) {
            rootCtx = ctx;

        } else {
            JDBCContext mostRecentCtx = getMostRecentContext();
            attach( mostRecentCtx, ctx );
        }

        updateConnection( ctx, tx );

        //add( ctx, tx );
        return ctx;
    }
    
    public Connection getNewConnection( DataSource ds, boolean autoCommit ) {
        
        try {
            Connection conn = OliveUtils.getConnection( ds, true );
            OliveUtils.setAutoCommit( conn, autoCommit );
            return conn;

        } catch ( Exception e ) {
            throw OliveUtils.closeSilently( e, conn );
        }
    }

    public void updateConnection( JDBCContext ctx, boolean tx ) {
        if ( tx ) {
            txConn = ctx.getConnection();

        } else {
            conn = ctx.getConnection();
        }

    }

//    public void updateMostRecentContext( JDBCContext ctx, boolean tx ) {
//
//        if ( tx ) {
//            txConn = ctx.getConnection();
//
//        } else {
//            conn = ctx.getConnection();
//        }
//
//        //mostRecentCtx = ctx;
//    }
//    public void onBeforeContextDetach( JDBCContext ctx ) {
//        mostRecentCtx = ctx.getParent();
//        //boolean tx = 
//    }
    public void attach( JDBCContext parent, JDBCContext child ) {

        if ( parent == null || child == null || parent == child) {
            return;
        }

        parent.attach( child );
    }

    public void detach( JDBCContext ctx ) {
        ctx.detach();

    }

    protected Connection getConnection( boolean tx ) {

//        JDBCContext ctx = peekTop( tx );
//
//        if ( ctx == null ) {
//            return null;
//        }
//
//        Connection conn = ctx.getConnection();
//        return conn;
        if ( tx ) {
            return txConn;

        } else {
            return conn;
        }
    }

    public boolean isEmpty() {
        return rootCtx == null;
        //return !ctxHolder.hasChild();
        //return ctxHolder == null;
    }

    protected void resetConnection( Connection connToReset ) {

        if ( connToReset == conn ) {
            conn = null;

        } else if ( connToReset == txConn ) {
            txConn = null;
        }
    }

//
//    public boolean isEmpty() {
//        if ( getStack().isEmpty() && getTxStack().isEmpty() ) {
//            return true;
//        }
//
//        return false;
//    }
//    @Override
//    public String toString() {
//        return getClass().getName() + "@" + Integer.toHexString( hashCode() ) + ", TxStack size: " + txStack.size() + ", TxStack size: " + stack.size();
//    }
    private class ManagerListener extends JDBCContextListener {

        @Override
        public void onBeforeContextDetach( JDBCContext ctx ) {

            if (ctx == rootCtx) {
                rootCtx = null;
            }

        }

        @Override
        public void onConnectionClosed( JDBCContext ctx) {

            if ( ctx.hasConnection() ) {
                Connection contextConn = ctx.getConnection();
                resetConnection( contextConn );
            }

        }

    }

}
