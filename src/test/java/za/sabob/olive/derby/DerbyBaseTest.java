package za.sabob.olive.derby;

import java.sql.*;
import javax.sql.*;
import org.testng.annotations.*;

public class DerbyBaseTest {

    protected DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = DerbyTestUtils.createDS();

        DerbyTestUtils.createPersonTable( ds );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        DriverManager.getConnection( "jdbc:derby:memory:olive;shutdown=true" );
    }
}
