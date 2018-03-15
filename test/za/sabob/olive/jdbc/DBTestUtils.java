package za.sabob.olive.jdbc;

import java.sql.*;
import javax.sql.*;
import org.h2.jdbcx.*;
import org.hsqldb.jdbc.*;
import za.sabob.olive.util.*;

public class DBTestUtils {

    public static int H2 = 1;

    public static int HSQLDB = 2;

    public static DataSource createDataSource( int db ) {
        return createDataSource( db, 10 );
    }

    public static DataSource createDataSource( int db, int poolSize ) {
        
        DataSource ds = null;
        
        if ( db == H2 ) {
            ds = getH2DataSource( poolSize );
            
        } else if (db == HSQLDB) {
            ds = getHSQLDataSource( poolSize );
        }
        
        return ds;
    }

    public static DataSource getH2DataSource( int poolSize ) {
        JdbcConnectionPool pool = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        pool.setMaxConnections( poolSize );
        pool.setLoginTimeout( 1 );
        return pool;
    }

    public static DataSource getHSQLDataSource( int poolSize ) {
        JDBCPool ds = new JDBCPool( poolSize );
        ds.setUrl( "jdbc:hsqldb:mem:." );

        //"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1;INIT=RUNSCRIPT FROM 'classpath:/za/co/momentum/reos/config/h2-create.sql'", "sa", "sa" );
        ds.setUser( "sa" );
        ds.setPassword( "" );
        try {
            ds.setLoginTimeout( 1 );
        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
        return ds;
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
            throw new RuntimeException( ex );
        } finally {
            OliveUtils.close( conn );
        }
    }

    public static void createPersonTable( DataSource ds, int db ) {

        if ( db == H2 ) {
            update( ds, "create table if NOT EXISTS person (id bigint auto_increment, name varchar(100), primary key (id));" );

        } else if ( db == HSQLDB ) {

            update( ds, "create table if not exists person (id bigint IDENTITY, name varchar(100), primary key (id));" );
        }
    }
}
