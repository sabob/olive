package za.sabob.olive.jdbc.operation;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.postgres.*;

public class OP_RestoreAutoCommitToTrue extends PostgresBaseTest {

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = PostgresTestUtils.createDS( 1 );
        System.out.println( "Postgres created" );
        PostgresTestUtils.createPersonTable( ds );
    }

    @Test
    public void closeConnectionTest() throws SQLException {
// ensure autoCommit is true
        Connection conn = ds.getConnection();
        conn.setAutoCommit( true );
        conn.close();

        JDBCContext ctx = JDBC.beginOperation( ds );
        Assert.assertTrue( ctx.getConnection().getAutoCommit() );

        JDBC.cleanupOperation( ctx );

        Assert.assertTrue( ds.getConnection().getAutoCommit() );
    }

}
