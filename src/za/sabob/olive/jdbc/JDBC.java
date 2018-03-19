package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;
import za.sabob.olive.util.*;

public class JDBC {

    private static final Logger LOGGER = Logger.getLogger( JDBC.class.getName() );

    public static JDBCContext beginOperation() {

        assertPreviousConnectionRegisterNotFaulty();

        DataSource ds = JDBCLookup.getDefaultDataSource();
        JDBCContext context = beginOperation( ds );

        return context;
    }

    public static JDBCContext beginOperation( DataSource ds ) {

        boolean hasConnection = false;
        Connection conn = null;

        try {

            assertPreviousConnectionRegisterNotFaulty();

            DataSourceContainer container = JDBCLookup.getDataSourceContainer();

            container.setFaultRegisteringDS( true );

            boolean isTransactional = false;
            hasConnection = container.hasConnection( ds, isTransactional );

            boolean transactional = false;
            conn = container.getConnection( ds, transactional );
            container.setActiveDataSource( ds );

            container.setFaultRegisteringDS( false );

            //validateConnection( conn );
            // Switch on autocommit in case it is off
            if ( !OliveUtils.getAutoCommit( conn ) ) {
                OliveUtils.setAutoCommit( conn, true );
            }

            boolean isRoot = !hasConnection; // if there is no existing conn, this conn is newly created, thus root
            JDBCContext context = new JDBCContext( conn, isRoot );
            return context;

        } catch ( Exception e ) {

            if ( hasConnection ) {
                throw e;
            }

            boolean autoCommit = true;
            throw OliveUtils.closeSilently( autoCommit, e, conn );
        }
    }

    public static boolean isAtRootConnection() {

        if ( !JDBCLookup.hasDataSourceContainer() ) {
            return false;
        }

        if ( isFaultRegisteringDS() ) {
            return false;
        }

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();

        if ( !container.hasActiveDataSource() ) {
            return false;
        }

        DataSource ds = container.getActiveDataSource();
        return isAtRootConnection( ds );
    }

    public static boolean isAtRootConnection( DataSource ds ) {

        if ( !JDBCLookup.hasDataSourceContainer() ) {
            return false;
        }

        if ( isFaultRegisteringDS() ) {
            return false;
        }

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();

        boolean transactional = false;
        return container.isAtRootConnection( ds, transactional );
    }

    public static void cleanupOperation( AutoCloseable... closeables ) {

        boolean isConnectionRegisterFaulty = isFaultRegisteringDS();
        clearConnectionRegisterFault();

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();

        Connection conn;

        List<Connection> conns = OliveUtils.getConnections( closeables );

        if ( conns.isEmpty() ) {

            if ( isConnectionRegisterFaulty ) {
                JDBCLookup.cleanup( closeables );
                return;
            }

            boolean transactional = false;
            conn = container.getLatestConnection( transactional );

            if ( conn == null ) {
                throw new IllegalArgumentException(
                    "No connection was found to clean up. Either pass a Connection as a parameter or use begin to register a connection with the operation." );
            }

            List<AutoCloseable> list = Arrays.asList( closeables );
            list = new ArrayList<>( list );
            list.add( conn );
            closeables = list.toArray( new AutoCloseable[list.size()] );

        } else {
            if ( conns.size() > 1 ) {
                throw new IllegalArgumentException( " Only 1 Connection can be passed to cleanupOperation" );
            }
            conn = conns.get( 0 );
        }

        DataSource ds = container.getActiveDataSource();

        boolean isAtRootConnection = isAtRootConnection( ds );

        boolean success = container.removeConnection( ds, conn );

        boolean hasConnections = container.hasConnections();

        if ( !hasConnections ) {
            JDBCLookup.unbindDataSourceContainer();
        }

        List resources = OliveUtils.removeConnections( closeables );
        Exception ex1 = JDBCLookup.cleanupSilently( resources );

        if ( isAtRootConnection ) {

            boolean autoCommit = true;

            List<Connection> connections = OliveUtils.getConnections( closeables );

            RuntimeException ex2 = OliveUtils.closeSilently( autoCommit, connections );

            ex1 = OliveUtils.addSuppressed( ex2, ex1 );
        }

        OliveUtils.throwAsRuntimeIfException( ex1 );
    }

    public static RuntimeException cleanupOperation( Exception exception, List<AutoCloseable> closeables ) {

        if ( closeables != null && closeables.size() > 0 ) {
            AutoCloseable[] autoCloseables = closeables.toArray( new AutoCloseable[closeables.size()] );
            return cleanupOperation( exception, autoCloseables );
        }

        return OliveUtils.toRuntimeException( exception );
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

        if ( !JDBCLookup.hasDataSourceContainer() ) {
            return false;
        }

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();
        return container.isFaultRegisteringDS();
    }

    public static void clearConnectionRegisterFault() {
        DataSourceContainer container = JDBCLookup.getDataSourceContainer();
        container.setFaultRegisteringDS( false );
    }

    public static void assertPreviousConnectionRegisterNotFaulty() {
        if ( isFaultRegisteringDS() ) {
            throw new IllegalStateException(
                "Seems that JDBC.beginOperation was previously invoked and failed to retrieve a connection and placed in an inconsistent state. JDBC.beginOperation "
                + "was invoked again without first calling JDBC.cleanupOperation. JDBC.cleanupOperation is required to cleanup resources and put the JDBC operations "
                + " in a consistent state." );
        }
    }
}
