package za.sabob.olive.jdbc.mixed.restore;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;

public class TX_RestoreAutoCommit extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws Exception {

        JDBCContext tx = JDBC.beginTransaction( ds );
        Assert.assertFalse( tx.getConnection().getAutoCommit() );

        JDBCContext op = JDBC.beginOperation( ds );
        Assert.assertFalse( op.getConnection().getAutoCommit() ); // beginning Operation doesn't change autoCommit

        JDBC.cleanupOperation( op );
        Assert.assertFalse( op.getConnection().getAutoCommit() );// cleaning up Operation doesn't change autoCommit
        Assert.assertFalse( op.isOpen() );
        Assert.assertFalse( op.isConnectionClosed() );

        JDBC.cleanupTransaction( tx );
        Assert.assertTrue( tx.isConnectionClosed() );

    }

}
