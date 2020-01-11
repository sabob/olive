package za.sabob.olive.util;

import org.testng.Assert;
import za.sabob.olive.jdbc.JDBCContext;
import za.sabob.olive.jdbc.util.JDBCUtils;

import java.sql.ResultSet;
import java.sql.Statement;

public class DBTestUtils {

    public static boolean isTimeout( Throwable ex ) {
        if ( ex.getMessage().contains( "Login timeout" ) || ex.getMessage().contains( "Invalid argument in JDBC call" )
                || ex.getMessage().contains( "connection does not exist" )
                || ex.getMessage().contains( "Connection has timed out." ) ) {
            return true;
        }
        return false;
    }

    public static void assertOpen( ResultSet rs ) {
        Assert.assertFalse( JDBCUtils.isClosed( rs ) );
    }

    public static void assertClosed( ResultSet rs ) {
        Assert.assertTrue( JDBCUtils.isClosed( rs ) );
    }

    public static void assertOpen( Statement stmt ) {
        Assert.assertFalse( JDBCUtils.isClosed( stmt ) );
    }

    public static void assertClosed( Statement stmt ) {
        Assert.assertTrue( JDBCUtils.isClosed( stmt ) );
    }

    public static void assertOpen( JDBCContext ctx ) {
        Assert.assertFalse( ctx.isClosed() );
        Assert.assertFalse( ctx.isConnectionClosed() );
    }

    public static void assertClosed( JDBCContext ctx ) {
        Assert.assertTrue( ctx.isClosed() );
        Assert.assertTrue( ctx.isConnectionClosed() );
    }

}
