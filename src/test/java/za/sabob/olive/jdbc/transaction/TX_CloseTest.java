package za.sabob.olive.jdbc.transaction;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;

public class TX_CloseTest extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext parent = JDBC.beginTransaction( ds );
        Assert.assertTrue( parent.isRootTransactionContext() );
        Assert.assertTrue( parent.canCommit() );

        JDBCContext child1 = JDBC.beginTransaction( ds );
        Assert.assertFalse( child1.isRootTransactionContext() );
        Assert.assertFalse( child1.canCommit() );

        Assert.assertFalse( parent.getConnection().isClosed() );

        JDBC.cleanupTransaction( child1 );
        Assert.assertFalse( parent.getConnection().isClosed() );

        Assert.assertFalse( child1.isRootTransactionContext() );
        JDBC.cleanupTransaction( child1 ); // should make no difference
        Assert.assertFalse( child1.isRootTransactionContext() );
        Assert.assertFalse( child1.isConnectionClosed() );

        Assert.assertFalse( child1.canCommit() );

        DataSourceContainer container = DSF.getDataSourceContainer();
        isEmpty = container.isEmpty( ds );
        Assert.assertFalse( isEmpty );

        JDBC.cleanupTransaction( child1 );
        Assert.assertFalse( parent.getConnection().isClosed() );

        Assert.assertTrue( parent.isRootTransactionContext() );
        Assert.assertTrue( parent.canCommit() );

        JDBC.cleanupTransaction( parent );
        Assert.assertTrue( parent.isConnectionClosed() );
        Assert.assertFalse( parent.isRootTransactionContext() );
        Assert.assertFalse( parent.canCommit() );

        isEmpty = container.isEmpty( ds );
        Assert.assertTrue( isEmpty );
    }

}
