package za.sabob.olive.jdbc2.close;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;

public class CloseTest extends PostgresBaseTest {

    @Test
    public void closeTest() throws SQLException {
        //  Closing parent context should close child and all references to JDBCContext should be removed from Olive eg. DatSourceContainer should be empty

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext ctx = JDBC.beginOperation( ds );

        ctx.close();

        DataSourceContainer container = DSF.getDataSourceContainer();
        isEmpty = container.isEmpty( ds );
        Assert.assertTrue( isEmpty );

        isEmpty = container.isEmpty();
        Assert.assertTrue( isEmpty );
    }

    @Test
    public void closeNestedParentTest() throws SQLException {
        //  Closing parent context should close child and all references to JDBCContext should be removed from Olive eg. DatSourceContainer should be empty

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext parent = JDBC.beginOperation( ds );

        JDBCContext child = JDBC.beginOperation( ds );

        parent.close();

        DataSourceContainer container = DSF.getDataSourceContainer();
        isEmpty = container.isEmpty( ds );
        Assert.assertTrue( isEmpty );

        isEmpty = container.isEmpty();
        Assert.assertTrue( isEmpty );
    }

    @Test
    public void closeNestedChildTest() throws SQLException {
        //  Closing child context should close child only, parent should *not* be removed from Olive eg. DatSourceContainer should *not* be empty

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext parent = JDBC.beginOperation( ds );

        JDBCContext child = JDBC.beginOperation( ds );

        child.close();

        DataSourceContainer container = DSF.getDataSourceContainer();
        isEmpty = container.isEmpty( ds );
        Assert.assertFalse( isEmpty );

        isEmpty = container.isEmpty();
        Assert.assertFalse( isEmpty );

        parent.close(); // make sure we cleanup by closing parent
    }

}
