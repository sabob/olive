package za.sabob.olive.transaction;

import java.sql.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class TransactionTest {

    public static void main( String[] args ) {

        Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            TX.beginTransaction( conn );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( conn, "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();

            if ( true ) {
                throw new RuntimeException( "moo" );
            }

            while ( rs.next() ) {
                System.out.println( "Row:" + rs.getString( "CATALOG_NAME" ) );
            }

            TX.commitTransaction(  conn );

        } catch ( SQLException ex ) {

            TX.rollbackTransactionAndThrow( conn, ex );

        } finally {
            TX.cleanupTransaction(conn, ps, rs );

        }
    }
}
