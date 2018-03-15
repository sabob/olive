package za.sabob.olive.jdbc;

import javax.sql.*;

public class JDBCContext {

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

    public static boolean hasJDBCContainer() {
        return HOLDER.get() != null;
    }

    public static void bindDataSourceContainer( DataSourceContainer container ) {
        HOLDER.set( container );
    }

    public static void unbindDataSourceContainer() {
        DataSourceContainer container = getDataSourceContainer();

        //container.popActiveDataSource(); // Not needed since cleanupTransaction should do this

        if (container.hasActiveDataSource()) {
            throw new IllegalStateException("DataSourceContainer should be empty, but contains an active DataSource. Make sure you cleanup all transactions with TX.cleanupTransaction()");
        }
        HOLDER.set( null );
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
        JDBCContext.defaultDataSource = defaultDataSource;
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