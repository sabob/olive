package za.sabob.olive.transaction;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.util.*;

public class OliveTransaction {

    public static Connection begin( Connection conn ) {
        OliveUtils.setAutoCommit( conn, false );
        return conn;
    }

    public static Connection begin( DataSource ds ) {
        Connection conn = OliveUtils.getConnection( ds );
        begin( conn );
        return conn;
    }

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

        Throwable mainException = null;

        try {
            OliveUtils.setAutoCommit( conn, true );
        } catch ( RuntimeException e ) {
            mainException = e.getCause();
        }

        try {
            OliveUtils.close( autoClosables );
        } catch ( RuntimeException e ) {

            Throwable closeException = e.getCause();
            mainException = OliveUtils.addSuppressed( closeException, mainException );
        }

        OliveUtils.throwAsRuntimeIfException( mainException );
    }

    public static void main( String[] args ) {

    }
}
