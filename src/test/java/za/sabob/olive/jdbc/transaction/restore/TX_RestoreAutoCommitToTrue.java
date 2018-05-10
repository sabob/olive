package za.sabob.olive.jdbc.transaction.restore;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;

public class TX_RestoreAutoCommitToTrue extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty();
        Assert.assertTrue( isEmpty );

        JDBCContext ctx = JDBC.beginTransaction( ds );
        Assert.assertFalse( ctx.getConnection().getAutoCommit() );

        JDBC.cleanupTransaction( ctx );

        Assert.assertTrue( ds.getConnection().getAutoCommit() );
    }

}
