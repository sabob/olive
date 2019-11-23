package za.sabob.olive.jdbc.postgres.multiple.close;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.postgres.*;

public class ConnectionStaysTX_Test extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        JDBCContext parentTX = JDBC.beginTransaction( ds );

        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );
        Assert.assertTrue( parentTX.isOpen());

        JDBCContext child1TX = JDBC.beginTransaction( ds );
        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );
        Assert.assertTrue( parentTX.isOpen());

        JDBCContext parentOP = JDBC.beginOperation( ds );
        Assert.assertTrue( parentOP.getConnection().getAutoCommit() );
        Assert.assertTrue( parentOP.isOpen());

        Assert.assertFalse( parentTX.getConnection().isClosed() );

        JDBC.cleanupTransaction( parentOP );
        Assert.assertFalse( parentTX.getConnection().isClosed() );
        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );

        JDBC.cleanupTransaction( child1TX );
        Assert.assertFalse( parentTX.getConnection().isClosed() );

        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );
        JDBC.cleanupTransaction( parentTX );

        Assert.assertTrue( parentTX.isConnectionClosed() );
    }

}
