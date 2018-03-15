package za.sabob.olive.jdbc;

import java.sql.*;
import javax.sql.*;
import org.h2.jdbcx.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class JDBCTest {

    JdbcDataSource ds;

    @BeforeClass
    public void beforeClass() {
        ds = new JdbcDataSource();
        ds.setURL( "jdbc:h2:~/test" );
        ds.setUser( "sa" );
        ds.setPassword( "sa" );

    }

    @Test
    public void basicTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            conn = JDBC.beginOperation( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( conn, "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "CATALOG_NAME" );
                System.out.println( "Row:" + name );
                Assert.assertEquals( name, "TEST" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {

            Assert.assertTrue( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( ps, rs );

            boolean isAtRoot = JDBC.isAtRootConnection();
            Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );

        }
    }

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void threadTest() {
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            conn = JDBC.beginOperation( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( conn, "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "CATALOG_NAME" );
                Assert.assertEquals( name, "TEST" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            Assert.assertTrue( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( ps, rs );

            boolean isAtRoot = JDBC.isAtRootConnection();
            Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }

    }

    public static void nested( DataSource ds ) {

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            Connection conn = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( conn, "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "CATALOG_NAME" );
                Assert.assertEquals( name, "TEST" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( ps, rs );
            Assert.assertTrue( JDBC.isAtRootConnection() );
        }

    }
}
