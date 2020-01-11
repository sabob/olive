package za.sabob.olive.jdbc.threads;

import za.sabob.olive.jdbc.JDBCContext;

import java.sql.*;
import javax.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;
import za.sabob.olive.jdbc.ps.*;
import za.sabob.olive.util.*;

public class ThreadJDBCTest extends PostgresBaseTest {

    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        ds = PostgresTestUtils.createDS( 20 );
        System.out.println( "Postgres created" );
        PostgresTestUtils.createPersonTable( ds );
        ds.setCheckoutTimeout( 2000 ); // There should be no deadlocks because Olive uses only 1 connection per thread.
    }

        @Test( successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000 )
    //@Test(successPercentage = 100, threadPoolSize = 2, invocationCount = 3, timeOut = 1110000)
    public void basicThreadTest() {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginOperation( ds );
            Assert.assertTrue( JDBCUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertTrue( ctx.isOpen() );

            nested( ds );

            Assert.assertTrue( JDBCUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertTrue( ctx.isOpen() );

            SqlParams params = new SqlParams();
            PreparedStatement ps = JDBCUtils.prepareStatement( ctx, "select * from person p", params );
            String name = JDBCUtils.mapToPrimitive( String.class, ps );

        } catch ( Exception e ) {
            throw new RuntimeException( e );

        } finally {

            if ( ctx != null ) {
                JDBC.cleanupOperation( ctx );
                Assert.assertTrue( ctx.isClosed() );
            }

        }

    }

    public static void nested( DataSource ds ) {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();
            PreparedStatement ps = JDBCUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
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

            if ( ctx != null ) {
                Assert.assertTrue( JDBCUtils.getAutoCommit( ctx.getConnection() ) );
                Assert.assertTrue( ctx.isOpen() );
            }

            JDBC.cleanupOperation( ctx );

            if ( ctx != null ) {
                Assert.assertTrue( ctx.isClosed() );
            }
        }

    }
}
