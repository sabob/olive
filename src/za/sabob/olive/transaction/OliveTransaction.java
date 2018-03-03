package za.sabob.olive.transaction;

import java.sql.*;
import java.util.logging.*;
import za.sabob.olive.transaction.sync.*;
import za.sabob.olive.util.*;

public class OliveTransaction {

    private static final Logger LOGGER = Logger.getLogger( OliveTransaction.class.getName() );

    public static Connection begin( Connection conn ) {

        JDBCContainer jdbcContainer = SynchronizedJDBC.getJDBCContainer();
        ConnectionContainer connContainer = jdbcContainer.getConnectionContainer( conn );

        if ( connContainer != null ) {
            LOGGER.warning( "OliveTransaction#begin discovered a ConnectionContainer with unclosed connections/statemens or resultSets. Closing them now!" );

            connContainer.close();
            jdbcContainer.removeContainer( conn );
        }

        jdbcContainer.createContainer( conn );

        OliveUtils.setAutoCommit( conn, false );
        return conn;
    }

//
//    public static Connection begin( DataSource ds ) {
//        Connection conn = OliveUtils.getConnection( ds );
//        begin( conn );
//        return conn;
//    }
    public static void commit( Connection conn ) {
        OliveUtils.commit( conn );
    }

    public static void rollback( Connection conn, Throwable ex ) {
        OliveUtils.rollback( conn, ex );
    }

    public static void close( AutoCloseable... autoClosables ) {

        Connection conn = OliveUtils.getConnection( autoClosables );
        if ( conn == null ) {
            throw new IllegalArgumentException( "At least one of the closables must be a Connection" );
        }

        JDBCContainer container = SynchronizedJDBC.getJDBCContainer();
        container.close();

        SynchronizedJDBC.unbindJDBCContainer();

        Throwable mainException = null;

        try {

            boolean autoCommit = true;
            OliveUtils.close( autoCommit, autoClosables );

        } catch ( RuntimeException e ) {

            Throwable closeException = e.getCause();
            mainException = OliveUtils.addSuppressed( closeException, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
    }

    public static void main( String[] args ) {
//        
//        try {
//            Connection conn = Olive.beginTransaction( ds );
//            
//            Statement st = Olive.createStatement(filename, params);
//            
//            Olive.queryForObject(st, rowMapper);
//            Olive.queryForList(st, rowMapper);
//            boolean success = Olive.insert(st);
//            Long id = Olive.insertForId(st);
//            boolean success = Olive.update(st);
//            boolean success = Olive.delete(st);
//            
//            
//        } catch ( Exception e ) {
//            Olive.rollbackTransaction(conn);
//        }
//        finally {
//            Olive.closeTransaction();
//            
//        }

    }
}
