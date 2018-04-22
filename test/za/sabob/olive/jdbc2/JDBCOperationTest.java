package za.sabob.olive.jdbc2;

import za.sabob.olive.jdbc2.context.JDBCContext;
import za.sabob.olive.util.DBTestUtils;
import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;

public class JDBCOperationTest {

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
