package za.sabob.olive.derby;

import com.mchange.v2.c3p0.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import javax.sql.*;
import org.testng.*;
import za.sabob.olive.jdbc2.*;

public class DerbyTestUtils {

    public static DataSource createDS() {
        int poolSize = 5;
        return createDS( poolSize );

    }

    public static DataSource createDS( int poolSize ) {
        DriverManager.setLoginTimeout( 1 );
        ComboPooledDataSource ds = new ComboPooledDataSource();

        try {

            //Driver d = org.apache.derby.jdbc.Driver;

            ds.setDriverClass( "org.apache.derby.jdbc.EmbeddedDriver" );
            ds.setLoginTimeout( 1 );

        } catch ( Exception ex ) {
            throw new RuntimeException( ex );
        }

        ds.setJdbcUrl( "jdbc:derby:memory:olive;create=true" );

        //ds.setUser( "postgres" );
        //ds.setPassword( "postgres" );
        ds.setInitialPoolSize( poolSize );
        ds.setMinPoolSize( poolSize );
        ds.setMaxPoolSize( poolSize );

        DSF.registerDefault( ds );

        return ds;

//
//        try {
//            EmbeddedConnectionPoolDataSource ds = new EmbeddedConnectionPoolDataSource();
//
//            ds.setDatabaseName( "memory:olive" );
//            ds.setCreateDatabase( "create" );
//            ds.setLoginTimeout( 1 );
//            //ds.setShutdownDatabase( "shutdown" );
//            return ds;
//
//        } catch ( SQLException ex ) {
//            throw new RuntimeException( ex );
//        }
    }

    public static void createPersonTable( DataSource ds ) {
        try {
            URI uri = DerbyTestUtils.class.getResource( "create-tables.sql" ).toURI();
            String ddl = new String( Files.readAllBytes( Paths.get( uri ) ) );
            //System.out.println( "DDL: " + ddl );

            Connection conn = ds.getConnection();
            Statement stmnt = conn.createStatement();
            stmnt.executeUpdate( ddl );

        } catch ( Exception ex ) {
            throw new RuntimeException( ex );
        }

    }

    public static void populateDatabase( DataSource ds ) {

        Connection conn;

        try {
            conn = ds.getConnection();
            Statement stmnt = conn.createStatement();
            String DML = "INSERT INTO person(name) VALUES ('bob')";
            stmnt = conn.createStatement();
            stmnt.executeUpdate( DML );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static void shutdown( DataSource ds ) {
        try {
            DSF.unbindDataSourceContainer();
            Assert.assertFalse( DSF.hasDataSourceContainer() );

            DriverManager.getConnection( "jdbc:derby:memory:olive;shutdown=true" );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }
}
