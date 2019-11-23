package za.sabob.olive.jdbc.basic;

import java.sql.Connection;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.JDBCContext;
import za.sabob.olive.postgres.PostgresBaseTest;
import za.sabob.olive.postgres.PostgresTestUtils;
import za.sabob.olive.ps.SqlParams;
import za.sabob.olive.util.OliveUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BasicJDBCTest extends PostgresBaseTest {

    @BeforeClass( alwaysRun = true )
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

            Connection conn = ctx.getConnection();
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( rs.isClosed() );
            Assert.assertTrue( ps.isClosed() );
            Assert.assertTrue( conn.isClosed() );

        } catch ( Exception e ) {
            throw e;

        } finally {

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

            Connection conn = ctx.getConnection();

            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( OliveUtils.isClosed( conn ) );
            Assert.assertTrue( ctx.getStatements().get( 0 ).isClosed() );

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
            Connection conn = ctx.getConnection();
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( OliveUtils.isClosed( conn ) );
        }

    }
}
