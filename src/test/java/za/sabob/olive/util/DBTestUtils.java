package za.sabob.olive.util;

import com.mchange.v2.c3p0.*;
import java.sql.*;
import javax.sql.*;
import org.hsqldb.jdbc.*;
import org.testng.*;
import za.sabob.olive.jdbc.*;

public class DBTestUtils {

    //public static int H2 = 1;

    public static int HSQLDB = 2;

    //public static int POSTGRES = 4;

    public static DataSource createDataSource( int db ) {
        return createDataSource( db, 10 );
    }

    public static DataSource createDataSource( int db, int poolSize ) {
        boolean multiThreaded = true;
        return createDataSource( db, 10, multiThreaded );
    }

    public static DataSource createDataSource( int db, int poolSize, boolean multiThreaded ) {

        int multiThreadedAsInt = multiThreaded ? 1 : 0;

        DataSource ds = null;

//        if ( db == H2 ) {
//            ds = getH2DataSource( poolSize, multiThreadedAsInt );
//
//        } else
if ( db == HSQLDB ) {
            ds = getHSQLDataSource( poolSize );

        }
//else if ( db == POSTGRES ) {
//            ds = getPostGresDataSource( poolSize );
//        }

        return ds;
    }
//
//    public static DataSource getH2DataSource( int poolSize, int multiThreadedInt ) {
//        JdbcConnectionPool pool = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=" + multiThreadedInt, "sa", "sa" );
//        pool.setMaxConnections( poolSize );
//        pool.setLoginTimeout( 1 );
//        return pool;
//    }

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

    public static void clearPersonTable( DataSource ds ) {
        update( ds, "delete from person" );
    }

    public static DataSource getPostGresDataSource( int poolSize ) {
        ComboPooledDataSource ds = new ComboPooledDataSource();

        try {

            ds.setDriverClass( "org.postgresql.Driver" );
            ds.setLoginTimeout( 1 );

        } catch ( Exception ex ) {
            throw new RuntimeException( ex );
        }

        ds.setJdbcUrl( "jdbc:postgresql://localhost:5432/olivedb" );
        ds.setUser( "sa" );
        ds.setPassword( "sa" );
        ds.setInitialPoolSize( poolSize );
        ds.setMinPoolSize( poolSize );
        ds.setMaxPoolSize( poolSize );
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

//        if ( db == H2 ) {
//            update( ds, "create table if NOT EXISTS person (id bigint auto_increment, name varchar(100), primary key (id));" );
//
//        } else
        if ( db == HSQLDB ) {

            update( ds, "create table if not exists person (id bigint IDENTITY, name varchar(100), primary key (id));" );
        }
    }

    public static boolean isTimeout( Throwable ex ) {
        if ( ex.getMessage().contains( "Login timeout" ) || ex.getMessage().contains( "Invalid argument in JDBC call" )
            || ex.getMessage().contains( "connection does not exist" ) ) {
            return true;
        }
        return false;
    }

    public static void shutdown( DataSource ds ) {
        try {
            ds.getConnection().createStatement().execute( "SHUTDOWN" );
            Assert.assertFalse( JDBCLookup.hasConnections( ds ) );
            Assert.assertFalse( JDBCLookup.hasDataSourceContainer() );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }

    }
}
