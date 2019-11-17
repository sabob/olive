package za.sabob.olive.jdbc.basic;

import za.sabob.olive.jdbc.context.JDBCContext;

import java.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.postgres.*;

public class JDBCOperationTest extends PostgresBaseTest {

    @Test
    public void beginCleanupTest() throws SQLException {

        JDBCContext ctx = JDBC.beginOperation( ds );
        JDBC.cleanupOperation( ctx );

        ctx = JDBC.beginOperation( ds );

        Assert.assertTrue( ctx.isOpen() );
        Assert.assertTrue( ctx.getConnection().getAutoCommit() );

        JDBC.cleanupOperation( ctx );

        Assert.assertTrue( ctx.getConnection().getAutoCommit() );
        Assert.assertTrue( ctx.getConnection().isClosed() );
        Assert.assertNull( ctx.isClosed() );
    }

    @Test
    public void beginCleanupNestedTest() throws SQLException {

        JDBCContext ctx1 = JDBC.beginOperation( ds );

        Assert.assertTrue( ctx1.getConnection().getAutoCommit() );

        JDBCContext ctx2 = JDBC.beginOperation( ds );

        JDBC.cleanupOperation( ctx1 );

        JDBC.cleanupOperation( ctx2 );

        Assert.assertTrue( ctx1.isClosed() );
        Assert.assertTrue( ctx2.isClosed() );
        Assert.assertTrue( ctx1.getConnection().isClosed() );
        Assert.assertTrue( ctx2.getConnection().isClosed() );
    }
}
