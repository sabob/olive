package za.sabob.olive.jdbc.mixed.restore;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;

public class OPS_RestoreAutoCommit extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws Exception {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty();
        Assert.assertTrue( isEmpty );

        JDBCContext op = JDBC.beginOperation( ds );
        Assert.assertTrue( op.getConnection().getAutoCommit() );

        JDBCContext tx = JDBC.beginTransaction( ds );
        Assert.assertFalse( tx.getConnection().getAutoCommit() );

        JDBC.cleanupTransaction( tx );
        Assert.assertTrue( tx.getConnection().getAutoCommit() ); // Switched autoCommit back to true
        Assert.assertFalse( tx.isOpen() );
        Assert.assertFalse( tx.isConnectionClosed() );

        JDBC.cleanupOperation( op );
        Assert.assertTrue( op.isConnectionClosed() );

    }

}
