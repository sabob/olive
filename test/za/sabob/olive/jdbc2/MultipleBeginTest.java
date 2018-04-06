package za.sabob.olive.jdbc2;

import za.sabob.olive.util.DBTestUtils;
import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;

public class MultipleBeginTest {

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
    public void basicTest() throws SQLException {

        boolean isEmpty = JDBCLookup.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext first = JDBC.beginOperation( ds );
        
        JDBCContext ctx = JDBC.beginOperation( ds );
        
        Assert.assertEquals(ctx.getParent(), first);
        Assert.assertEquals(first.getChild(), ctx);

        //ctx = JDBC.beginOperation( ds );
        //read();
        //insert();
        JDBCContext lastCreatedCtx = save();

        JDBCContext mostRecentCtx = ctx.getMostRecentContext();

        Assert.assertFalse( ctx.isRootContext() );
        Assert.assertFalse( ctx.getRootContext().getConnection().isClosed() );

        Assert.assertFalse( ctx.getConnection().isClosed() );
        
        JDBCContext parent = ctx.getParent();
        Assert.assertNotNull(parent );
        
        JDBCContext rootParent = parent.getParent();
        Assert.assertNotNull(rootParent );
        
        ctx.close();

        Assert.assertFalse( ctx.getConnection().isClosed(), "Context' Connection must not be closed because a second context was created which is root." );

        Assert.assertTrue( ctx.isRootContext(), "Context must be root since it was created first!" );
        Assert.assertEquals( lastCreatedCtx, mostRecentCtx, "Last context created should match the deepest child!" );
        Assert.assertEquals( ctx.getConnection(), mostRecentCtx.getConnection(), "Contexts must share the same connection!" );
    }

    public JDBCContext save() {
        JDBCContext ctx = JDBC.beginOperation( ds );
        return read();
    }

    public JDBCContext read() {
        JDBCContext ctx = JDBC.beginOperation( ds );
        return ctx;
    }
}
