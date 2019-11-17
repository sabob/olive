package za.sabob.olive.jdbc.close;

import java.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;

public class CloseTest extends PostgresBaseTest {

    @Test
    public void closeTest() throws SQLException {

        JDBCContext ctx = JDBC.beginOperation( ds );
        Assert.assertTrue( ctx.getConnection().getAutoCommit() );

        ctx.close();

        Assert.assertTrue( ctx.isClosed() );
        Assert.assertTrue( ctx.getConnection().getAutoCommit() );
        Assert.assertTrue( ctx.isConnectionClosed() );
    }

    @Test
    public void closeNestedParentTest() throws SQLException {
        //  Closing parent context should close child and all references to JDBCContext should be removed from Olive eg. DatSourceContainer should be empty

        JDBCContext first = JDBC.beginOperation( ds );
        JDBCContext second = JDBC.beginOperation( ds );

        first.close();

        Assert.assertTrue( first.isClosed() );
        Assert.assertTrue( second.isOpen() );
    }

    @Test
    public void closeNestedChildTest() throws SQLException {
        //  Closing child context should close child only, parent should *not* be removed from Olive eg. DatSourceContainer should *not* be empty

        JDBCContext first = JDBC.beginOperation( ds );

        JDBCContext second = JDBC.beginOperation( ds );

        second.close();

        Assert.assertTrue( first.isOpen() );
        Assert.assertTrue( second.isClosed() );

        first.close(); // make sure we cleanup by closing parent
        Assert.assertTrue( first.isClosed() );
    }

}
