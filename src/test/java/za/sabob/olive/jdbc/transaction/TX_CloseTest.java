package za.sabob.olive.jdbc.transaction;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;

public class TX_CloseTest extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        JDBCContext first = JDBC.beginTransaction( ds );
        Assert.assertFalse( JDBCUtils.getAutoCommit( first.getConnection() ) );
        Assert.assertTrue( first.isOpen() );

        JDBCContext second = JDBC.beginTransaction( ds );
        Assert.assertFalse( JDBCUtils.getAutoCommit( second.getConnection() ) );

        Assert.assertFalse( first.getConnection().isClosed() );

        JDBC.cleanupTransaction( second );
        Assert.assertTrue( first.getConnection().isClosed() );
        Assert.assertTrue( JDBCUtils.getAutoCommit( second.getConnection() ) );

        JDBC.cleanupTransaction( second ); // should make no difference
        Assert.assertTrue( JDBCUtils.getAutoCommit( second.getConnection() ) );
        Assert.assertTrue( second.isConnectionClosed() );

        Assert.assertFalse( first.getConnection().isClosed() );
        Assert.assertFalse( JDBCUtils.getAutoCommit( first.getConnection() ) );

        JDBC.cleanupTransaction( first );

        Assert.assertTrue( JDBCUtils.getAutoCommit( first.getConnection() ) );
        Assert.assertTrue( first.isConnectionClosed() );
        Assert.assertTrue( first.isClosed() );
    }

}
