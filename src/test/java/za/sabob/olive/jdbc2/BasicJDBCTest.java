package za.sabob.olive.jdbc2;

import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class BasicJDBCTest extends PostgresBaseTest {

    @BeforeClass(alwaysRun = true)
    public void beforeThisClass() {
        PostgresTestUtils.populateDatabase( ds );
    }

    @Test
    public void basicNonRecommendedTest() throws SQLException {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = JDBC.beginOperation( ds );
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
            ctx.add( ps );

            rs = ps.executeQuery();
            ctx.add( rs );
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                Assert.assertEquals( name, "bob" );
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

    @Test
    public void basicRecommendedTest() throws SQLException {

        JDBCContext ctx = JDBC.beginOperation( ds );

        try {

            nested( ds );

            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "select name from person p", params ); // PreparedStatement is added to JDBCContext to close automatically
            String name = OliveUtils.mapToPrimitive( String.class, ps ); // The underlying ResultSet will be clsoed automatically
            Assert.assertEquals( name, "bob" );

        } finally {

            Assert.assertTrue( ctx.isRootContext() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( ctx.getStatements().get( 0 ).isClosed() );
            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );

        }
    }

    public static void nested( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );

        try {

            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
            ctx.add( ps );

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
            Assert.assertFalse( ctx.isRootContext() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isRootContext() );
        }

    }
}
