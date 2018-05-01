package za.sabob.olive.jdbc2.mixed;

import java.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;

public class MixedCloseConnectionTest extends PostgresBaseTest {

    @Test
    public void closeConnectionTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext nonTxParent = JDBC.beginOperation( ds );
        JDBCContext txParent = JDBC.beginTransaction( ds ); // Note: nonTxParent is the parent of txParent since it was created first

        JDBCContext nonTxChild1 = JDBC.beginOperation( ds );

        JDBCContext txChild3 = JDBC.beginTransaction( ds );

        JDBCContext txChild4 = JDBC.beginTransaction( ds );

        JDBCContext nonTxChild5 = JDBC.beginOperation( ds );

        JDBCContext nonTxChild6 = JDBC.beginOperation( ds );

        JDBCContext nonTxChild7 = JDBC.beginOperation( ds );

        JDBCContext txChild8 = JDBC.beginTransaction( ds );

        JDBC.cleanupOperation( nonTxChild1 );
        JDBC.cleanupOperation( nonTxChild7 );
        Assert.assertFalse( nonTxParent.getConnection().isClosed() );

        JDBC.cleanupTransaction( txChild3 );
        JDBC.cleanupTransaction( txChild8 );
        Assert.assertFalse( txParent.getConnection().isClosed() );
        Assert.assertFalse( nonTxParent.getConnection().isClosed() );
        Assert.assertFalse( nonTxParent.isClosed() );
        Assert.assertFalse( txParent.isClosed() );

        JDBC.cleanupOperation( nonTxParent ); // because nonTxParent is the root of both nonTx and TX connections, closing it will close all other contexts, nonTx and TX ones.
        Assert.assertTrue( txParent.getConnection().isClosed() );
        Assert.assertTrue( nonTxParent.getConnection().isClosed() );

        Assert.assertTrue( nonTxParent.isClosed() );
        Assert.assertTrue( txParent.isClosed() );

        Assert.assertTrue( txChild8.isClosed() );
        Assert.assertTrue( nonTxChild7.isClosed() );
    }

}
