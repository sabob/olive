package za.sabob.olive.jdbc2;

import za.sabob.olive.jdbc2.context.JDBCContext;
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

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext parent = JDBC.beginOperation( ds );

        JDBCContext child = JDBC.beginOperation( ds );

        Assert.assertEquals( child.getParent(), parent );
        Assert.assertEquals( parent.getChild(), child );
        Assert.assertNull( parent.getParent() );

        //ctx = JDBC.beginOperation( ds );
        //read();
        //insert();
        JDBCContext lastCreatedCtx = save();

        JDBCContext mostRecentCtx = child.getMostRecentContext();

        Assert.assertFalse( child.isRootContext() );
        Assert.assertFalse( child.getRootContext().getConnection().isClosed() );

        Assert.assertFalse( child.getConnection().isClosed() );

        JDBCContext childParent = child.getParent();
        Assert.assertNotNull( childParent );

        JDBCContext rootParent = childParent.getParent();
        Assert.assertNull( rootParent );

        child.close();

        Assert.assertFalse( child.getConnection().isClosed(), "child' Connection must not be closed because a parent context was created which is root." );
        Assert.assertTrue( child.isRootContext(), "Context must be root now because its parent was removed when it was closed!" );
        Assert.assertEquals( lastCreatedCtx, mostRecentCtx, "Last context created should match the deepest child!" );
        Assert.assertEquals( child.getConnection(), mostRecentCtx.getConnection(), "Contexts must share the same connection!" );

        parent.close();
        
        Assert.assertTrue( parent.getConnection().isClosed(), "connection should be closed now because parent was closed.");
        Assert.assertTrue( child.getConnection().isClosed(), "Child connection should be closed now because parent was closed.");
        Assert.assertTrue( child.isRootContext(), "Context must be root since it was created first!" );
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
