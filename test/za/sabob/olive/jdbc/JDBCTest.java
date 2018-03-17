package za.sabob.olive.jdbc;

import za.sabob.olive.util.DBTestUtils;
import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class JDBCTest {

    DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = DBTestUtils.createDataSource(DBTestUtils.H2);
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }
    
    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        ds.getConnection().createStatement().execute( "SHUTDOWN" );
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
            ps = OliveUtils.prepareStatement( conn, "select * from person p", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                Assert.assertEquals( name, "Bob" );
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
    public void basicThreadTest() {
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            conn = JDBC.beginOperation( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( conn, "select * from person p", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                //Assert.assertEquals( name, "TEST" );
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
            ps = OliveUtils.prepareStatement( conn, "select * from person p", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                //Assert.assertEquals( name, "TEST" );
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
