package za.sabob.olive.postgres;

import com.mchange.v2.c3p0.*;
import com.opentable.db.postgres.embedded.*;
import com.opentable.db.postgres.embedded.EmbeddedPostgres.Builder;

import java.net.*;
import java.nio.file.*;
import java.sql.*;
import javax.sql.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.config.OliveConfig;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class PostgresTestUtils {

    public static EmbeddedPostgres pg;

    public static void shutdown( DataSource ds ) {

        try {

            Thread.sleep( 500 ); // Not sure why but give a bit of time so Postgres dont shut down while connections still busy.
            pg.close();

        } catch ( Exception ex ) {
            throw new RuntimeException( ex );
        }

    }

    public static ComboPooledDataSource createDS() {
        return createDS( 5 );

    }

    public static ComboPooledDataSource createDS( int poolSize ) {

        try {

            Builder builder = EmbeddedPostgres.builder()
            .setPort( 45678 )
            .setCleanDataDirectory( true );
            //builder.setDataDirectory(new File( "c:/applogs/postgres/olive"));
            pg = builder.start();


            //String url = pg.getJdbcUrl( "postgres", "postgres" );
//            DataSource ds = pg.getPostgresDatabase();
//            Connection c = ds.getConnection();
//            Connection c2 = ds.getConnection();
//
//            Statement s = c.createStatement();
//            ResultSet rs = s.executeQuery( "SELECT 1" );
//            assertTrue( rs.next() );
//            assertEquals( 1, rs.getInt( 1 ) );
//            assertFalse( rs.next() );
            //pg.close();
            ComboPooledDataSource ds = new ComboPooledDataSource();

            ds.setDriverClass( "org.postgresql.Driver" );
            ds.setCheckoutTimeout( 1000 );

            //ds.setJdbcUrl( "jdbc:postgresql://localhost:45678/postgres?user=postgres&loginTimeout=1&connectTimeout=1&socketTimeout=1" );
            ds.setJdbcUrl( "jdbc:postgresql://localhost:45678/postgres?user=postgres" );
            ds.setAcquireRetryAttempts( poolSize );
            ds.setBreakAfterAcquireFailure( true );

            ds.setUser( "postgres" );
            ds.setPassword( "postgres" );
            ds.setInitialPoolSize( poolSize );
            ds.setMinPoolSize( poolSize );
            ds.setMaxPoolSize( poolSize );

            //ds.getConnection().close();

            OliveConfig.setDefaultDataSource( ds );

            return ds;

        } catch ( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static void createPersonTable( DataSource ds ) {

        JDBC.inTransaction( ds, ( ctx ) -> {

            URI uri = PostgresTestUtils.class.getResource( "create-tables.sql" ).toURI();
            String ddl = new String( Files.readAllBytes( Paths.get( uri ) ) );
            //System.out.println( "DDL: " + ddl );

            Statement stmnt = OliveUtils.createStatement( ctx );
            stmnt.executeUpdate( ddl );
        } );

    }

    public static void populateDatabase( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );

            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count;
            count = ps.executeUpdate();

            params.set( "name", "john" );
            count = ps.executeUpdate();

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        } finally {
            ctx.close();
        }
    }

    public static void main( String[] args ) throws Exception {
        DataSource ds = createDS( 45 );
        createPersonTable( ds );
        populateDatabase( ds );

        Connection conn = ds.getConnection();
        System.out.println( "conn " + conn );

        //conn = ds.getConnection();
        System.out.println( "conn " + conn );

        //DataSource ds = ds.getPostgresDatabase();
        Connection c = ds.getConnection();
        Connection c2 = ds.getConnection();

        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery( "SELECT * from person" );
        assertTrue( rs.next() );
        assertEquals( "Bob", rs.getString( "name" ) );

        conn.close();
        c.close();
        c2.close();
        shutdown( ds );

    }
}
