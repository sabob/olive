package za.sabob.olive.transaction;

import java.sql.*;
import java.util.logging.*;
import za.sabob.olive.util.*;

public class OliveTransaction {

    private static final Logger LOGGER = Logger.getLogger( OliveTransaction.class.getName() );

    public static Connection beginTransaction( Connection conn ) {
        OliveUtils.setAutoCommit( conn, false );
        return conn;
    }

    public static void commitTransaction( Connection conn ) {
        OliveUtils.commit( conn );
    }

    public static void rollbackTransaction( Connection conn, Throwable ex ) {
        OliveUtils.rollback( conn, ex );
    }

    public static void closeTransaction( AutoCloseable... autoClosables ) {

        Connection conn = OliveUtils.getConnection( autoClosables );
        if ( conn == null ) {
            throw new IllegalArgumentException( "At least one of the closables must be a Connection" );
        }

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
}
