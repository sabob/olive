package za.sabob.olive.jdbc;

import java.util.*;
import java.util.logging.*;
import javax.sql.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.util.*;

/**
 * DataSourceFactory
 */
public class DSF {

    private final static Logger LOGGER = Logger.getLogger( DSF.class.getName() );

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

    public static boolean hasJDBCContexts( DataSource ds ) {
        if ( !hasDataSourceContainer() ) {
            return false;
        }

        DataSourceContainer container = getDataSourceContainer();

        return !container.isEmpty( ds );
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
        if ( !container.isEmpty() ) {
            throw new IllegalStateException(
                "DataSourceContainer should be empty, but contains an active JDBCContext'. Make sure you cleanup all transactions with JDBC.cleanupTransaction()" );
        }
        HOLDER.set( null );
    }

    public static JDBCContext getLatestJDBCContext( DataSource ds ) {

        if ( !hasDataSourceContainer() ) {
            throw new IllegalStateException( "There is no Connection registered. Use JDBC.beginTransaction or JDBC.beginOperation to create a connection." );
        }

        DataSourceContainer container = getDataSourceContainer();
        JDBCContext ctx = container.getManager( ds ).getMostRecentContext();

        if ( ctx == null ) {
            throw new IllegalStateException(
                "There is no JDBCContext available. Use TX.beginTransaction or JDBC.beginOperation to create a JDBCContext in your service layer." );
        }

        return ctx;
    }

//    public static DataSource getLatestDataSource() {
//
//        if ( !hasDataSourceContainer() ) {
//            throw new IllegalStateException( "There is no DataSource registered. Use TX.beginTransaction or JDBC.beginOperation to register a dataSource." );
//        }
//
//        DataSourceContainer container = getDataSourceContainer();
//
//        boolean hasActiveDS = container.hasActiveDataSource();
//
//        if ( hasActiveDS ) {
//            return container.getActiveDataSource();
//        }
//
//        return null;
//    }
    public static boolean hasDefault() {
        return defaultDataSource != null;
    }

    public static DataSource getDefault() {

        if ( defaultDataSource == null ) {
            throw new IllegalStateException(
                "No default DataSource have been registered. Register a default "
                + "DataSource with DSF.registerDefault() or provide the DataSource to the JDBC methods. " );
        }

        return defaultDataSource;
    }

    public static void registerDefault( DataSource defaultDataSource ) {
        if ( DSF.defaultDataSource != null ) {
            LOGGER.warning( "DSF.registerDefault() called while an existing defaultDataSource has already been set before" );

        }
        DSF.defaultDataSource = defaultDataSource;
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
        OliveUtils.close( closeables );
    }

    public static RuntimeException cleanupQuietly( Iterable<AutoCloseable> closeables ) {
        return cleanup( null, closeables );

    }

    public static RuntimeException cleanupQuietly( AutoCloseable... closables ) {
        return cleanup( null, closables );
    }

    public static RuntimeException cleanupQuietly( Exception exception, Iterable<AutoCloseable> closeables ) {
        return cleanup( null, closeables );
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
