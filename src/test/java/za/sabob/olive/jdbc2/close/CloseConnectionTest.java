package za.sabob.olive.jdbc2.close;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.util.*;

public class CloseConnectionTest {

    DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = DBTestUtils.createDataSource( DBTestUtils.H2 );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        DBTestUtils.shutdown( ds );
    }

    @Test
    public void closeConnectionTest() throws SQLException {

        boolean isEmpty = DSF.getDataSourceContainer().isEmpty( ds );
        Assert.assertTrue( isEmpty );

        JDBCContext parent = JDBC.beginOperation( ds );

        JDBCContext child1 = JDBC.beginOperation( ds );

        JDBCContext child2 = JDBC.beginOperation( ds );

        Assert.assertFalse( parent.getConnection().isClosed() );

        JDBC.cleanupOperation( child2 );
        Assert.assertFalse( parent.getConnection().isClosed() );

        JDBC.cleanupOperation( child1 );
        Assert.assertFalse( parent.getConnection().isClosed() );

        JDBC.cleanupOperation( parent );
        Assert.assertTrue( parent.getConnection().isClosed() );
    }

}
