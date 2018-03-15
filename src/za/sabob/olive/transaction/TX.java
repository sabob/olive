package za.sabob.olive.transaction;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;
import za.sabob.olive.jdbc.*;
import static za.sabob.olive.jdbc.JDBC.isAtRootConnection;
import za.sabob.olive.util.*;

public class TX {

    private static final Logger LOGGER = Logger.getLogger( TX.class.getName() );

    public static Connection beginTransaction() {

        assertPreviousConnectionRegisterNotFaulty();

        DataSource ds = JDBCContext.getDefaultDataSource();
        Connection conn = beginTransaction( ds );
        return conn;
    }

    public static Connection beginTransaction( DataSource ds ) {

        assertPreviousConnectionRegisterNotFaulty();

        DataSourceContainer container = JDBCContext.getDataSourceContainer();

        container.setFaultRegisteringDS( true );
        //boolean hasConnection = container.hasConnection( ds );

        boolean transactional = true;
        Connection conn = container.getConnection( ds, transactional );
        container.setActiveDataSource( ds );

        container.setFaultRegisteringDS( false );

        //if ( hasConnection ) {
        //validateConnection( conn );
        //}
        if ( OliveUtils.getAutoCommit( conn ) ) {
            OliveUtils.setAutoCommit( conn, false );
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

    public static RuntimeException rollbackTransactionSilently( Connection conn ) {
        return OliveUtils.rollbackSilently( conn );
    }

    public static RuntimeException rollbackTransactionSilently() {
        if ( isFaultRegisteringDS() ) {
            return null;
        }

        Connection conn = getLatestConnection();
        return rollbackTransactionSilently( conn );
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

    public static RuntimeException rollbackTransactionSilently( Exception ex ) {
        if ( isFaultRegisteringDS() ) {
            return null;
        }

        Connection conn = getLatestConnection();
        return rollbackTransactionSilently( conn, ex );
    }

    public static void cleanupTransaction( AutoCloseable... autoClosables ) {

        boolean isConnectionRegisterFaulty = isFaultRegisteringDS();
        clearConnectionRegisterFault();

        DataSourceContainer container = JDBCContext.getDataSourceContainer();

        Connection conn = OliveUtils.getConnection( autoClosables );
        if ( conn == null ) {

            if ( isConnectionRegisterFaulty ) {
                return;
            }

            boolean transactional = true;
            conn = container.getLatestConnection( transactional );

            if ( conn == null ) {
                throw new IllegalArgumentException(
                    "No connection was found to clean up. Either pass a Connection as a parameter or use beginTransaction to register a connection with the transaction." );
            }

            List<AutoCloseable> list = Arrays.asList(autoClosables );
            list = new ArrayList<>( list );
            list.add( conn );
            autoClosables = list.toArray( new AutoCloseable[list.size()] );
        }

        DataSource ds = container.getActiveDataSource();

        //boolean isAtRootConnection = isAtRootConnection( ds );

        boolean success = container.removeConnection( ds, conn );

        if ( !container.hasConnections() ) {
            JDBCContext.unbindDataSourceContainer();

            boolean autoCommit = true;
            OliveUtils.close( autoCommit, autoClosables );

        }
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
        DataSourceContainer container = JDBCContext.getDataSourceContainer();

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
                "TX.beginTransaction was invoked and failed to retrieve a connection and was invoked AGAIN without first calling TX.cleanupTransaction." );
        }
    }
}
