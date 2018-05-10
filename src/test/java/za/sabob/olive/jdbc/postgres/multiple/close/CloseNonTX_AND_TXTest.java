package za.sabob.olive.jdbc.postgres.multiple.close;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;

public class CloseNonTX_AND_TXTest extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext parentTX = JDBC.beginTransaction( ds );
        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );
        Assert.assertTrue( parentTX.isRootTransactionContext() );
        Assert.assertTrue( parentTX.canCommit() );
        Assert.assertTrue( parentTX.isRootConnectionHolder() );

        JDBCContext child1TX = JDBC.beginTransaction( ds );
        Assert.assertFalse( child1TX.isRootTransactionContext() );
        Assert.assertFalse( child1TX.canCommit() );

        JDBCContext parentOP = JDBC.beginOperation( ds );
        Assert.assertFalse( parentOP.isRootConnectionHolder() );

        Assert.assertFalse( parentTX.getConnection().isClosed() );

        JDBC.cleanupTransaction( parentOP );
        Assert.assertFalse( parentTX.getConnection().isClosed() );
        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );

        DataSourceContainer container = DSF.getDataSourceContainer();
        isEmpty = container.isEmpty( ds );
        Assert.assertFalse( isEmpty );

        JDBC.cleanupTransaction( child1TX );
        Assert.assertFalse( parentTX.getConnection().isClosed() );

        Assert.assertFalse( parentTX.getConnection().getAutoCommit() );
        JDBC.cleanupTransaction( parentTX );

        Assert.assertTrue( parentTX.isConnectionClosed() );

        isEmpty = container.isEmpty( ds );
        Assert.assertTrue( isEmpty );
    }

}
