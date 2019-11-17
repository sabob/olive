package za.sabob.olive.jdbc.threads;

import java.sql.*;
import javax.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class ThreadTXTest extends PostgresBaseTest {

    @Test( successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000 )
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
            Assert.assertFalse( OliveUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertTrue( ctx.isOpen() );
            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isClosed() );
        }

    }

    public static void nested( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

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

            Assert.assertFalse( OliveUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertTrue( ctx.isOpen() );
            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( ctx.isConnectionClosed() );
        }

    }
}
