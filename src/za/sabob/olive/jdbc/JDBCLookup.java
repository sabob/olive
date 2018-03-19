package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import za.sabob.olive.util.*;

public class JDBCLookup {

    private static DataSource defaultDataSource;

    private static final ThreadLocal<DataSourceContainer> HOLDER = new ThreadLocal<DataSourceContainer>();

    public static DataSourceContainer getDataSourceContainer() {

        DataSourceContainer container = HOLDER.get();

        if ( container == null ) {
            container = new DataSourceContainer();
            bindDataSourceContainer( container );
        }

        return container;
    }

    public static boolean isManagedConnection( Connection conn ) {

        if ( !hasDataSourceContainer() ) {
            return false;
        }

        DataSourceContainer container = getDataSourceContainer();
        boolean contains = container.containsConnection( conn );
        return contains;
    }

    public static boolean hasConnections( DataSource ds ) {
        if ( !hasDataSourceContainer() ) {
            return false;
        }

        DataSourceContainer container = getDataSourceContainer();

        boolean hasConnecions = container.hasConnection( ds );
        return hasConnecions;
    }

    public static boolean hasDataSourceContainer() {
        return HOLDER.get() != null;
    }

    public static void bindDataSourceContainer( DataSourceContainer container ) {
        HOLDER.set( container );
    }

    public static void unbindDataSourceContainer() {
        DataSourceContainer container = getDataSourceContainer();

        //container.popActiveDataSource(); // Not needed since cleanupTransaction should do this
        if ( container.hasActiveDataSource() ) {
            throw new IllegalStateException(
                "DataSourceContainer should be empty, but contains an active DataSource. Make sure you cleanup all transactions with TX.cleanupTransaction()" );
        }
        HOLDER.set( null );
    }

    public static Connection getLatestConnection( DataSource ds ) {

        if ( !hasDataSourceContainer() ) {
            throw new IllegalStateException( "There is no Connection registered. Use TX.beginTransaction or JDBC.beginOperation to create a connection." );
        }

        DataSourceContainer container = getDataSourceContainer();
        Connection conn = container.getLatestConnection( ds );

        if ( conn == null ) {
            throw new IllegalStateException(
                "There is no Connection available. Use TX.beginTransaction or JDBC.beginOperation to create a connection in your service layer." );
        }

        return conn;
    }

    public static DataSource getLatestDataSource() {

        if ( !hasDataSourceContainer() ) {
            throw new IllegalStateException( "There is no DataSource registered. Use TX.beginTransaction or JDBC.beginOperation to register a dataSource." );
        }

        DataSourceContainer container = getDataSourceContainer();

        boolean hasActiveDS = container.hasActiveDataSource();

        if ( hasActiveDS ) {
            return container.getActiveDataSource();
        }

        return null;
    }

    public static boolean hasDefaultDataSource() {
        return defaultDataSource != null;
    }

    public static DataSource getDefaultDataSource() {
        if ( defaultDataSource == null ) {
            throw new IllegalStateException(
                "You did not provide a DataSource, so attempting to use JDBCContext.getDefaultDataSource() but no default DataSource is specified. Either pass a DataSource with your calls, or use JDBCContext.setDefaultDataSource() to set a default DataSource" );
        }
        return defaultDataSource;
    }

    public static void setDefaultDataSource( DataSource defaultDataSource ) {
        JDBCLookup.defaultDataSource = defaultDataSource;
    }

    public static void cleanup( AutoCloseable... closeables ) {
        List list = Arrays.asList( closeables );
        cleanup( list );
    }

    public static RuntimeException cleanup( Exception exception, AutoCloseable... autoClosables ) {
        List list = Arrays.asList( autoClosables );
        return cleanup( exception, list );
    }

    public static RuntimeException cleanup( Exception exception, Iterable<AutoCloseable> closeables ) {

        try {
            cleanup( closeables );
            return OliveUtils.toRuntimeException( exception );

        } catch ( Exception ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
            return OliveUtils.toRuntimeException( exception );
        }

    }

    public static void cleanup( Iterable<AutoCloseable> closeables ) {
        try {
            List<Connection> connections = OliveUtils.getConnections( closeables );
            List resources = OliveUtils.removeConnections( closeables );

            OliveUtils.close( resources );

            for ( Connection conn : connections ) {
                if ( !isManagedConnection( conn ) ) {
                    OliveUtils.close( conn );
                }
            }

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }
    
    public static RuntimeException cleanupSilently( Iterable<AutoCloseable> closeables ) {
        return cleanup( null, closeables);
        
    }

    public static RuntimeException cleanupSilently( AutoCloseable... closables ) {
        return cleanup( null, closables);
    }

    public static RuntimeException cleanupSilently( Exception exception, Iterable<AutoCloseable> closeables ) {
        return cleanup( null, closeables);
    }

//    
//    public void saveInServiceTX( Object o ) {
//
//        doTransactional( new TransactionCallback() {
//            @Override
//            public void execute() throws Exception {
//                
//                saveInDao( o );
//                
//            }
//        } );
//    }
//
//    public void saveInServiceNOTX( Object o ) {
//
//        doJDBCOperation( new JDBCCallback() {
//            @Override
//            public void execute() throws Exception {
//                
//                saveInDao( o );
//                
//            }
//        } );
//    }
//
//    public void saveInService2( Object o ) {
//
//        try {
//            TX.beginTransaction( ds );
//
//            saveInDao( o );
//
//            TX.commitTransaction();
//
//        } catch ( Exception e ) {
//            throw TX.rollbackTransaction();
//
//        } finally {
//            TX.cleanupTransaction( autoClosables );
//        }
//    }
//
//    public void saveInDao( Object o ) {
//
//        //TX.beginTransaction( conn );
//    }
}
