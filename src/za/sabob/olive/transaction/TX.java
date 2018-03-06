package za.sabob.olive.transaction;

import java.sql.*;
import java.util.logging.*;
import za.sabob.olive.util.*;

public class TX {

    private static final Logger LOGGER = Logger.getLogger( TX.class.getName() );

    public static Connection beginTransaction( Connection conn ) {
        OliveUtils.setAutoCommit( conn, false );
        return conn;
    }

    public static void commitTransaction( Connection conn ) {
        OliveUtils.commit( conn );
    }

    public static void rollbackTransaction( Connection conn ) {
        OliveUtils.rollback( conn );
    }

    public static RuntimeException rollbackTransactionSilently( Connection conn ) {
        return OliveUtils.rollbackSilently( conn );
    }

    public static RuntimeException rollbackTransaction( Connection conn, Exception ex ) {
        return OliveUtils.rollback( conn, ex );
    }

    public static RuntimeException rollbackTransactionSilently( Connection conn, Exception ex ) {
        return OliveUtils.rollbackSilently( conn, ex );
    }

    public static void cleanupTransaction( AutoCloseable... autoClosables ) {

        Connection conn = OliveUtils.getConnection( autoClosables );
        if ( conn == null ) {
            throw new IllegalArgumentException( "At least one of the closables must be a Connection" );
        }

        boolean autoCommit = true;
        OliveUtils.close( autoCommit, autoClosables );
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
}
