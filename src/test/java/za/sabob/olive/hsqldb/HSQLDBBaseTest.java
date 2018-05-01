package za.sabob.olive.hsqldb;

import javax.sql.*;
import org.testng.annotations.*;

public class HSQLDBBaseTest {

    protected DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = HSQLDBTestUtils.createDS();

        HSQLDBTestUtils.createPersonTable( ds );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        HSQLDBTestUtils.shutdown( ds );
    }
}
