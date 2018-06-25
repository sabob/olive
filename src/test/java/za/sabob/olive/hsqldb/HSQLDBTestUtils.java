package za.sabob.olive.hsqldb;

import java.sql.*;
import javax.sql.*;
import org.hsqldb.jdbc.*;
import org.testng.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.util.*;

public class HSQLDBTestUtils {

    public static DataSource createDS() {
        int poolSize = 5;
        return createDS( poolSize );
    }

    public static DataSource createDS( int poolSize ) {
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

        DSF.registerDefault( ds );
        return ds;
    }

    public static void createPersonTable( DataSource ds ) {

        JDBC.inTransaction( ds, (ctx) -> {
            String ddl = "create table if not exists person (id bigint IDENTITY, name varchar(100), primary key (id));";
            Statement stmnt = OliveUtils.createStatement( ctx );
            stmnt.executeUpdate( ddl );
        } );
    }

    public static void shutdown( DataSource ds ) {
        try {
            DSF.unbindDataSourceContainer();
            Assert.assertFalse( DSF.hasDataSourceContainer() );

            ds.getConnection().createStatement().execute( "SHUTDOWN" );

        } catch ( Throwable ex ) {
            System.out.println( "SHUTDOWN ERROR: " + ex.getMessage() );
            ex.printStackTrace();
        }
    }

    public static boolean isTimeout( Exception ex ) {
        if ( ex.getMessage().contains( "Login timeout" ) || ex.getMessage().contains( "Invalid argument in JDBC call" ) ) {
            return true;
        }
        return false;
    }
}
