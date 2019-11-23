package za.sabob.olive.jdbc.close;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.postgres.*;

public class CloseConnectionTest extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        JDBCContext parent = JDBC.beginOperation( ds );

        JDBCContext child1 = JDBC.beginOperation( ds );

        JDBCContext child2 = JDBC.beginOperation( ds );

        Assert.assertFalse( parent.getConnection().isClosed() );

        JDBC.cleanupOperation( child2 );
        Assert.assertFalse( parent.getConnection().isClosed() );

        JDBC.cleanupOperation( child1 );
        Assert.assertFalse( parent.getConnection().isClosed() );

        Connection conn = parent.getConnection();
        JDBC.cleanupOperation( parent );
        Assert.assertTrue( conn.isClosed() );
    }

}
