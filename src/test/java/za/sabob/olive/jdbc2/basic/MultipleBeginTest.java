package za.sabob.olive.jdbc2.basic;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.DSF;
import za.sabob.olive.jdbc2.JDBC;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;

public class MultipleBeginTest extends PostgresBaseTest {


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

        Assert.assertTrue( DSF.hasDataSourceContainer() );
        Assert.assertTrue( DSF.getDataSourceContainer().isEmpty( ds ) );
        Assert.assertTrue( DSF.getDataSourceContainer().isEmpty() );

        JDBC.cleanupOperation( ds );
        Assert.assertTrue( DSF.getDataSourceContainer().isEmpty() );

        JDBC.cleanupOperation( ds );
        JDBC.cleanupOperation( ds );
        JDBC.cleanupOperation( ds );
        JDBC.cleanupOperation( ds );
        JDBC.cleanupOperation( ds );
        JDBC.cleanupOperation( ds );
        Assert.assertTrue( DSF.getDataSourceContainer().isEmpty() );

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
