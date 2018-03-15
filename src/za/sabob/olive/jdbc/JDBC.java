package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;
import za.sabob.olive.util.*;

public class JDBC {

    private static final Logger LOGGER = Logger.getLogger( JDBC.class.getName() );

    public static Connection beginOperation() {

        assertPreviousConnectionRegisterNotFaulty();

        DataSource ds = JDBCContext.getDefaultDataSource();
        Connection conn = beginOperation( ds );
        return conn;
    }

    public static Connection beginOperation( DataSource ds ) {

        assertPreviousConnectionRegisterNotFaulty();

        DataSourceContainer container = JDBCContext.getDataSourceContainer();

        container.setFaultRegisteringDS( true );

        boolean transactional = false;
        Connection conn = container.getConnection( ds, transactional );
        container.setActiveDataSource( ds );

        container.setFaultRegisteringDS( false );

        //validateConnection( conn );

        // Switch on autocommit in case it is off
        if ( !OliveUtils.getAutoCommit( conn ) ) {
            OliveUtils.setAutoCommit( conn, true );
        }

        return conn;
    }

    public static boolean isAtRootConnection() {
        if ( isFaultRegisteringDS() ) {
            return false;
        }

        DataSourceContainer container = JDBCContext.getDataSourceContainer();

        if ( !container.hasActiveDataSource() ) {
            return false;
        }

        DataSource ds = container.getActiveDataSource();
        return isAtRootConnection( ds );
    }

    public static boolean isAtRootConnection( DataSource ds ) {
        DataSourceContainer container = JDBCContext.getDataSourceContainer();
        
        boolean transactional = false;
        return container.isAtRootConnection( ds, transactional );
    }

    public static void cleanupOperation( AutoCloseable... autoClosables ) {

        boolean isFaultRegisteringDS = isFaultRegisteringDS();
        clearConnectionRegisterFault();

        DataSourceContainer container = JDBCContext.getDataSourceContainer();

        Connection conn = OliveUtils.getConnection( autoClosables );

        if ( conn == null ) {

            if ( isFaultRegisteringDS ) {
                return;
            }

            boolean transactional = false;
            conn = container.getLatestConnection( transactional );

            if ( conn == null ) {
                throw new IllegalArgumentException(
                    "No connection was found to clean up. Either pass a Connection as a parameter or use begin to register a connection with the operation." );
            }

            List<AutoCloseable> list = Arrays.asList( autoClosables );
            list = new ArrayList<>( list );
            list.add( conn );
            autoClosables = list.toArray( new AutoCloseable[list.size()] );
        }

        DataSource ds = container.getActiveDataSource();

        //boolean isAtRootConnection = isAtRootConnection( ds );

        boolean success = container.removeConnection( ds, conn );

        if ( !container.hasConnections() ) {
        //if ( isAtRootConnection ) {
            JDBCContext.unbindDataSourceContainer();

            boolean autoCommit = true;
            OliveUtils.close( autoCommit, autoClosables );

        }
    }

    public static RuntimeException cleanupOperation( Exception exception, AutoCloseable... autoClosables ) {
        try {
            cleanupOperation( autoClosables );

        } catch ( Exception e ) {
            exception = OliveUtils.addSuppressed( e, exception );
        }
        return OliveUtils.toRuntimeException( exception );
    }

    public static RuntimeException cleanupOperationSilently( Exception exception, AutoCloseable... autoClosables ) {

        try {
            cleanupOperation( autoClosables );
            return OliveUtils.toRuntimeException( exception );

        } catch ( Exception ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
            return OliveUtils.toRuntimeException( exception );
        }
    }

    public static RuntimeException cleanupOperationSilently( AutoCloseable... autoClosables ) {
        return cleanupOperationSilently( null, autoClosables );
    }

    public static void validateConnection( Connection conn ) {

        if ( !OliveUtils.getAutoCommit( conn ) ) {
            throw new IllegalStateException( "The connection retrieved from JDBCContext has autoCommit set to FALSE, which is a transactional connection! "
                + "JDBC only works with NON Transactional connections. Ensure you call TX.cleanupTransaction or JDBC.cleanupOperation when finished with DB operations." );
        }
    }

    public static boolean isFaultRegisteringDS() {
        DataSourceContainer container = JDBCContext.getDataSourceContainer();
        return container.isFaultRegisteringDS();
    }

    public static void clearConnectionRegisterFault() {
        DataSourceContainer container = JDBCContext.getDataSourceContainer();
        container.setFaultRegisteringDS( false );
    }

    public static void assertPreviousConnectionRegisterNotFaulty() {
        if ( isFaultRegisteringDS() ) {
            throw new IllegalStateException(
                "JDBC.beginOperation was invoked and failed to retrieve a connection and was invoked AGAIN without first calling JDBC.cleanupOperation." );
        }
    }
}
