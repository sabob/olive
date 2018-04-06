package za.sabob.olive.jdbc2;

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
        ds = DBTestUtils.createDataSource( DBTestUtils.H2 );
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        DBTestUtils.shutdown( ds );
    }

    @Test
    public void basicTest() throws SQLException {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            ctx = JDBC.beginOperation( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
            ctx.add( ps );

            rs = ps.executeQuery();
            ctx.add( rs );
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                Assert.assertEquals( name, "Bob" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {

            Assert.assertTrue( ctx.isRootContext() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( rs.isClosed() );
            Assert.assertTrue( ps.isClosed() );

            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );

        }
    }

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void basicThreadTest() {


        JDBCContext ctx = null;
        
        try {
            ctx = JDBC.beginOperation( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
             ctx.add( ps);

            ResultSet rs = ps.executeQuery();
            ctx.add(rs);
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                //Assert.assertEquals( name, "TEST" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            Assert.assertTrue( ctx.isRootContext());
            JDBC.cleanupOperation( ctx );

            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }

    }

    public static void nested( DataSource ds ) {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
            ctx.add(ps);

            ResultSet rs = ps.executeQuery();
            ctx.add( rs );
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                //Assert.assertEquals( name, "TEST" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            Assert.assertFalse( ctx.isRootContext());
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isRootContext());
        }

    }
}
