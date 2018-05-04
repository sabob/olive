package za.sabob.olive.jdbc2.threads;

import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.JDBC;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class ThreadTXTest extends PostgresBaseTest {

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void basicThreadTest() {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginTransaction( ds );

            nested( ds );
            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "select * from person p", params );
            String name = OliveUtils.mapToPrimitive( String.class, ps );

        } catch ( Exception e ) {
            throw new RuntimeException( e );

        } finally {
            boolean isRoot = ctx.isRootContext();
            Assert.assertTrue( isRoot );
            JDBC.cleanupTransaction( ctx );

            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }

    }

    public static void nested( DataSource ds ) {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginTransaction( ds );

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
            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isRootContext() );
        }

    }
}
