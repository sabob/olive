package za.sabob.olive.jdbc2.basic;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.DSF;
import za.sabob.olive.jdbc2.JDBC;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;

public class JDBCOperationTest extends PostgresBaseTest {

    @Test
    public void beginCleanupTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext ctx = JDBC.beginOperation( ds );
        JDBC.cleanupOperation( ds );

        ctx = JDBC.beginOperation( ds );
        JDBC.cleanupOperation( ds );

        Assert.assertEquals( ctx.getParent(), null );
        Assert.assertEquals( ctx.getChild(), null );
        Assert.assertTrue( ctx.isClosed() );
        Assert.assertTrue( ctx.isRootContext() );
        Assert.assertTrue( ctx.getConnection().isClosed() );
        Assert.assertTrue( ctx.getRootContext().getConnection().isClosed() );
        Assert.assertNull( ctx.getParent() );
    }

    @Test
    public void beginCleanupNestedTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext ctx = JDBC.beginOperation( ds );

        //Assert.assertEquals( ctx.getParent().getParent(), null );
        Assert.assertEquals( ctx.getParent(), null );

        ctx = JDBC.beginOperation( ds );
        JDBCContext recent = ctx.getMostRecentContext();

        Assert.assertEquals( ctx, recent );

        JDBC.cleanupOperation( ds );

        JDBC.cleanupOperation( ds );

        Assert.assertEquals( ctx.getParent(), null );
        Assert.assertEquals( ctx.getChild(), null );
        Assert.assertTrue( ctx.isClosed() );
        Assert.assertTrue( ctx.isRootContext() );
        Assert.assertTrue( ctx.getConnection().isClosed() );
        Assert.assertTrue( ctx.getRootContext().getConnection().isClosed() );
        Assert.assertNull( ctx.getParent() );
    }
}
