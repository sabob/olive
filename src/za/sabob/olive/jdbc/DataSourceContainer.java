package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import javax.sql.*;
import za.sabob.olive.util.*;

public class DataSourceContainer { // implements AutoCloseable {

    final private Map<DataSource, ConnectionStack> dataSourceMap = new LinkedHashMap<>();

    final private Stack<DataSource> activeDataSourceStack = new Stack<>();

    private boolean faultRegisteringDS;

    public DataSourceContainer() {
    }

    public void removeDataSource( DataSource ds ) {
        ConnectionStack stack = getConnections( ds );
        if ( !stack.isEmpty() ) {
            throw new IllegalStateException(
                "Cannot remove dataSource if here are still active connections. Make sure you cleanup all connections with TX.cleanupTransaction() or JDBCOperation.cleanup()" );
        }
        dataSourceMap.remove( ds );
    }

    public boolean removeConnection( DataSource ds, Connection conn ) {
        ConnectionStack stack = getConnections( ds );

        boolean removed = stack.remove( conn );

        if ( stack.isEmpty() ) {
            //connectionMap.remove( conn );
            removeDataSource( ds );
        }

        popActiveDataSource();

        return removed;
    }

    public void removeConnection( Connection conn ) {
        DataSource ds = getActiveDataSource();
        removeConnection( ds, conn );

    }

    public ConnectionStack addDataSource( DataSource ds ) {
        ConnectionStack connectionStack = new ConnectionStack();
        dataSourceMap.put( ds, connectionStack );
        return connectionStack;
    }

    public Connection getConnection( boolean transactional ) {

        DataSource ds = getActiveDataSource();

        Connection conn = getConnection( ds, transactional );
        return conn;
    }

    public boolean hasConnection( DataSource ds ) {
        ConnectionStack stack = getConnections( ds );

        if ( stack == null || stack.isEmpty() ) {
            return false;
        }

        return true;
    }

    public boolean isAtRootConnection( DataSource ds, boolean transactional ) {
        ConnectionStack connections = getConnections( ds );

        if ( connections == null ) {
            return false;
        }

        return connections.isAtRootConnection( transactional );
    }

    public Connection getConnection( DataSource ds, boolean transactional ) {
        ConnectionStack connections = getConnections( ds );

        if ( connections == null ) {
            connections = addDataSource( ds );
        }

        Connection conn = connections.peekTop( transactional );

        if ( conn == null ) {
            conn = OliveUtils.getConnection( ds );
        }

        connections.add( conn );
        //connectionMap.put( conn, ds );

        return conn;

    }

//    public Connection getConnection( DataSource ds ) {
//        boolean transactional = false;
//        return getConnection( ds, transactional );
//    }
    
    public Connection getLatestConnection( ) {
        DataSource ds = getActiveDataSource();
        
        ConnectionStack connections = getConnections( ds );

        if ( connections == null ) {
            connections = addDataSource( ds );
        }

        Connection conn = connections.peekTop( );
        
        if (conn == null) {
            throw new IllegalStateException("There is no connection registered. Use TX.beginTransaction or JDBC.beginOperation to create a connection.");
        }

        return conn;        
    }
    
    
    public Connection getLatestConnection( DataSource ds, boolean transactional ) {
        ConnectionStack connections = getConnections( ds );
        return connections.peekTop( transactional );
    }

    public Connection getLatestConnection( boolean transactional ) {
        DataSource ds = getActiveDataSource();
        return getLatestConnection( ds, transactional );

    }

    public ConnectionStack getConnections( DataSource ds ) {
        ConnectionStack stack = dataSourceMap.get( ds );
        return stack;
    }

    public ConnectionStack getConnections() {

        DataSource ds = getActiveDataSource();

        ConnectionStack stack = dataSourceMap.get( ds );
        return stack;
    }

//    public DataSource getDataSource( Connection conn ) {
//        DataSource ds = connectionMap.get( conn );
//
//        if ( ds == null ) {
//            throw new IllegalStateException(
//                "there is no dataSource registered for the given connection. Ensure the connection was created through TX.beginTransaction( dataSource )." );
//        }
//        return ds;
//    }
//    @Override
//    public void close() {
//
//        List<AutoCloseable> closeables = collectCloseablesInReverseOrder();
//
//        OliveUtils.close( closeables );
//    }
    public DataSource getActiveDataSource() {

        DataSource ds = activeDataSourceStack.peekTop();

        if ( ds == null ) {
            return JDBCContext.getDefaultDataSource();
        }

        return ds;
    }

    public void setActiveDataSource( DataSource activeDataSource ) {
        activeDataSourceStack.add( activeDataSource );
    }

    public boolean hasActiveOrDefaultDataSource() {
        if ( hasActiveDataSource() ) {
            return true;
        }

        if ( JDBCContext.hasDefaultDataSource() ) {
            return true;
        }
        return false;
    }

    public boolean hasActiveDataSource() {
        return !activeDataSourceStack.isEmpty();
    }

    public boolean hasConnections() {
        if ( dataSourceMap.isEmpty() ) {
            return false;
        }

        for ( Entry<DataSource, ConnectionStack> entry : dataSourceMap.entrySet() ) {

            ConnectionStack stack = entry.getValue();

            if ( !stack.isEmpty() ) {
                return true;
            }
        }
        return false;
    }

    public DataSource popActiveDataSource() {
        DataSource ds = activeDataSourceStack.pop();
        return ds;
    }

    public boolean isFaultRegisteringDS() {
        return faultRegisteringDS;
    }

    public void setFaultRegisteringDS( boolean faultRegisteringDS ) {
        this.faultRegisteringDS = faultRegisteringDS;
    }
}
