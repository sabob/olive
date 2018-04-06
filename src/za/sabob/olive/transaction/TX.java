package za.sabob.olive.transaction;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.util.*;

public class TX {

    private static final Logger LOGGER = Logger.getLogger( TX.class.getName() );

    public static JDBCContext beginTransaction() {

        assertPreviousConnectionRegisterNotFaulty();

        DataSource ds = JDBCLookup.getDefaultDataSource();
        JDBCContext context = beginTransaction( ds );
        return context;
    }

    public static JDBCContext beginTransaction( DataSource ds ) {

        boolean hasConnection = false;
        Connection conn = null;

        try {

            assertPreviousConnectionRegisterNotFaulty();

            DataSourceContainer container = JDBCLookup.getDataSourceContainer();
            container.setFaultRegisteringDS( true );

            boolean isTransactional = true;
            hasConnection = container.hasConnection( ds, isTransactional );

            boolean transactional = true;

            conn = container.getConnection( ds, transactional );

            container.setActiveDataSource( ds );

            container.setFaultRegisteringDS( false );

            //if ( hasConnection ) {
            //validateConnection( conn );
            //}
            //OliveUtils.setTransactionIsolation( conn, Connection.TRANSACTION_READ_UNCOMMITTED);
            if ( OliveUtils.getAutoCommit( conn ) ) {
                OliveUtils.setAutoCommit( conn, false );
            }

            boolean isRoot = !hasConnection; // if there is no existing conn, this conn is newly created, thus root
            JDBCContext context = new JDBCContext( conn, isRoot );
            return context;

        } catch ( RuntimeException e ) {

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

        boolean transactional = true;
        return container.isAtRootConnection( ds, transactional );
    }

    public static void commitTransaction() {
        if ( isFaultRegisteringDS() ) {
            return;
        }

        Connection conn = getLatestConnection();
        commitTransaction( conn );
    }

    public static void commitTransaction( Connection conn ) {

        if ( !isAtRootConnection() ) {
            return;
        }

        OliveUtils.commit( conn );
    }

    public static void commitTransaction( JDBCContext ctx ) {
        Connection conn = ctx.getConnection();
        commitTransaction( conn );
    }

    public static void rollbackTransaction() {
        if ( isFaultRegisteringDS() ) {
            return;
        }

        Connection conn = getLatestConnection();
        rollbackTransaction( conn );
    }

    public static void rollbackTransaction( Connection conn ) {
        OliveUtils.rollback( conn );
    }

    public static void rollbackTransaction( JDBCContext ctx ) {
        Connection conn = ctx.getConnection();
        rollbackTransaction( conn );
    }

    public static RuntimeException rollbackTransactionSilently( Connection conn ) {
        return OliveUtils.rollbackSilently( conn );
    }

    public static RuntimeException rollbackTransactionSilently( JDBCContext ctx ) {
        Connection conn = ctx.getConnection();
        return OliveUtils.rollbackSilently( conn );
    }

    public static RuntimeException rollbackTransactionSilently() {
        if ( isFaultRegisteringDS() ) {
            return null;
        }

        Connection conn = getLatestConnection();
        return rollbackTransactionSilently( conn );
    }

    public static RuntimeException rollbackTransaction( JDBCContext ctx, Exception ex ) {
        Connection conn = ctx.getConnection();
        return rollbackTransaction( conn, ex );
    }

    public static RuntimeException rollbackTransaction( Connection conn, Exception ex ) {
        return OliveUtils.rollback( conn, ex );
    }

    public static RuntimeException rollbackTransaction( Exception ex ) {
        if ( isFaultRegisteringDS() ) {
            return null;
        }

        Connection conn = getLatestConnection();
        return rollbackTransaction( conn, ex );
    }

    public static RuntimeException rollbackTransactionSilently( Connection conn, Exception ex ) {
        return OliveUtils.rollbackSilently( conn, ex );
    }

    public static RuntimeException rollbackTransactionSilently( JDBCContext ctx, Exception ex ) {
        Connection conn = ctx.getConnection();
        return rollbackTransactionSilently( conn, ex );
    }

    public static RuntimeException rollbackTransactionSilently( Exception ex ) {
        if ( isFaultRegisteringDS() ) {
            return null;
        }

        Connection conn = getLatestConnection();
        return rollbackTransactionSilently( conn, ex );
    }

    public static void cleanupTransaction( JDBCContext ctx ) {

        if ( ctx == null ) {
            return;
        }
        
        List list = ctx.gatherResources();
        cleanupTransaction( list );

    }

    public static void cleanupTransaction( AutoCloseable... closeables ) {
        List<AutoCloseable> list = new ArrayList( Arrays.asList( closeables ));
        cleanupTransaction( list );
    }

    public static void cleanupTransaction( List<AutoCloseable> closeables ) {

        boolean isConnectionRegisterFaulty = isFaultRegisteringDS();
        clearConnectionRegisterFault();

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();

        Connection conn;

        List<Connection> conns = OliveUtils.findConnections( closeables );

        if ( conns.isEmpty() ) {

            if ( isConnectionRegisterFaulty ) {
                JDBCLookup.cleanup( closeables );
                return;
            }

            boolean transactional = true;
            conn = container.getLatestConnection( transactional );

            if ( conn == null ) {
                throw new IllegalArgumentException(
                    "No connection was found to clean up. Either pass a Connection as a parameter or use beginTransaction to register a connection with the transaction." );
            }

            closeables.add( conn );

        } else {
            if ( conns.size() > 1 ) {
                throw new IllegalArgumentException( " Only 1 Connection can be passed to cleanupTransaction" );
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

            List<Connection> connections = OliveUtils.findConnections( closeables );

            RuntimeException ex2 = OliveUtils.closeSilently( autoCommit, connections );

            ex1 = OliveUtils.addSuppressed( ex2, ex1 );
        }

        OliveUtils.throwAsRuntimeIfException( ex1 );
    }

    public static RuntimeException cleanupTransaction( Exception exception, AutoCloseable... autoClosables ) {
        try {
            cleanupTransaction( autoClosables );

        } catch ( Exception e ) {
            exception = OliveUtils.addSuppressed( e, exception );
        }
        return OliveUtils.toRuntimeException( exception );
    }

    public static RuntimeException cleanupTransactionSilently( Exception exception, AutoCloseable... autoClosables ) {

        try {
            cleanupTransaction( autoClosables );
            return OliveUtils.toRuntimeException( exception );

        } catch ( Exception ex ) {
            exception = OliveUtils.addSuppressed( ex, exception );
            return OliveUtils.toRuntimeException( exception );
        }
    }

    public static RuntimeException cleanupTransactionSilently( AutoCloseable... autoClosables ) {
        return cleanupTransactionSilently( null, autoClosables );
    }

    public static Connection getLatestConnection() {
        DataSourceContainer container = JDBCLookup.getDataSourceContainer();

        boolean transactional = true;
        Connection conn = container.getLatestConnection( transactional );
        return conn;
    }

    public static void validateConnection( Connection conn ) {

        if ( OliveUtils.getAutoCommit( conn ) ) {
            throw new IllegalStateException( "The connection retrieved from JDBCContext has autoCommit set to TRUE, which is not a transactional connection! "
                + "TX only works with Transactional connections. Ensure you call TX.cleanupTransaction or JDBC.cleanupOperation when finished with DB operations." );
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
                "Seems that TX.beginTransaction was previously invoked and failed to retrieve a connection and placed in an inconsistent state. TX.beginTransaction "
                + "was invoked again without first calling TX.cleanupTransaction. TX.cleanupTransaction is required to cleanup resources and put the TX operations "
                + " in a consistent state." );
        }
    }
}
