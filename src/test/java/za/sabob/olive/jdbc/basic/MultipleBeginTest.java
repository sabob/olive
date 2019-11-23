package za.sabob.olive.jdbc.basic;

import org.testng.Assert;
import org.testng.annotations.Test;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.JDBCContext;
import za.sabob.olive.postgres.PostgresBaseTest;

import java.sql.SQLException;

public class MultipleBeginTest extends PostgresBaseTest {


    @Test
    public void basicTest() throws SQLException {

        JDBCContext first = JDBC.beginOperation( ds );
        JDBCContext second = JDBC.beginOperation( ds );

        JDBCContext lastCreatedCtx = save();
        Assert.assertFalse( second.isConnectionClosed() );

        Assert.assertFalse( second.getConnection().isClosed() );

        second.close();

        Assert.assertTrue( second.getConnection().isClosed() );

        first.close();

        Assert.assertTrue( first.getConnection().isClosed(), "connection should be closed now because parent was closed." );
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
