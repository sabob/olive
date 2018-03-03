package za.sabob.olive.transaction;

import java.sql.*;
import static za.sabob.olive.Olive.BEGIN;
import static za.sabob.olive.Olive.CLOSE;
import static za.sabob.olive.Olive.COMMIT;
import static za.sabob.olive.Olive.ROLLBACK_AND_THROW;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class TransactionTest {

    public static void main( String[] args ) {

        Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            BEGIN( conn );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( conn, "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();

            if ( true ) {
                throw new RuntimeException( "moo" );
            }

            while ( rs.next() ) {
                System.out.println( "Row:" + rs.getString( "CATALOG_NAME" ) );
            }

            COMMIT( conn );

        } catch ( SQLException ex ) {

            ROLLBACK_AND_THROW( conn, ex );

        } finally {
            CLOSE( conn, ps, rs );

        }
    }
}
