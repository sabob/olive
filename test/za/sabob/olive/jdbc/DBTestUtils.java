package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.logging.*;
import javax.sql.*;
import org.h2.jdbcx.*;
import za.sabob.olive.util.*;

public class DBTestUtils {

    public static DataSource createDataSource() {
        return createDataSource( 10 );
    }

    public static DataSource createDataSource( int poolSize ) {
        JdbcConnectionPool pool = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        pool.setMaxConnections( poolSize );
        pool.setLoginTimeout( 1 );
        return pool;
    }

    public static void update( DataSource ds, String expression ) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            Statement st = null;
            st = conn.createStatement();    // statements
            int i = st.executeUpdate( expression );    // run the query
            if ( i == -1 ) {
                System.out.println( "db error : " + expression );
            }

            OliveUtils.close( st );
        } catch ( SQLException ex ) {
            Logger.getLogger( DBTestUtils.class.getName() ).log( Level.SEVERE, null, ex );
        } finally {
            OliveUtils.close( conn );
        }

    }

    public static void createPersonTable( DataSource ds ) {
        update( ds,
                "create table if NOT EXISTS person (id bigint auto_increment, name varchar(100), primary key (id));" );
    }
}
