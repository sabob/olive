package za.sabob.olive.jdbc.postgres.multiple.close;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.postgres.*;

public class CloseNonTX_AND_TXTest extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        JDBCContext firtTX = JDBC.beginTransaction( ds );
        Assert.assertFalse( firtTX.getConnection().getAutoCommit() );

        JDBCContext secondTX = JDBC.beginTransaction( ds );

        JDBCContext firstOP = JDBC.beginOperation( ds );

        Assert.assertFalse( firtTX.getConnection().isClosed() );

        JDBC.cleanupTransaction( firstOP );
        Assert.assertFalse( firtTX.getConnection().isClosed() );
        Assert.assertFalse( firtTX.getConnection().getAutoCommit() );

        JDBC.cleanupTransaction( secondTX );
        Assert.assertFalse( firtTX.getConnection().isClosed() );

        Assert.assertFalse( firtTX.getConnection().getAutoCommit() );
        JDBC.cleanupTransaction( firtTX );

        Assert.assertTrue( firtTX.isConnectionClosed() );
    }

}
