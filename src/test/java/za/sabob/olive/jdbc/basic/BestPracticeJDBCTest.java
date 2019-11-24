package za.sabob.olive.jdbc.basic;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import za.sabob.olive.Mode;
import za.sabob.olive.Olive;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.config.JDBCConfig;
import za.sabob.olive.postgres.PostgresBaseTest;
import za.sabob.olive.postgres.PostgresTestUtils;
import za.sabob.olive.ps.SqlParams;
import za.sabob.olive.util.OliveUtils;

import java.sql.PreparedStatement;

public class BestPracticeJDBCTest extends PostgresBaseTest {

    @BeforeClass( alwaysRun = true )
    public void beforeThisClass() {
        PostgresTestUtils.populateDatabase( ds );
    }

    @Test
    public void bestPracticeTest() {

        // This test shows how to load a SQL statement from a file and perform the SQL operation.
        // No Exception handling or try/catch logic need to be declared. The Connection, Statement and ResultSet will be closed automatically

        Olive olive = new Olive( Mode.DEVELOPMENT );

        JDBC.inOperation( ds, ( ctx ) -> {

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );
            String path = OliveUtils.path( getClass(), "BestPracticeJDBCTest.sql" );
            PreparedStatement ps = olive.prepareStatementFromFile( ctx, path, params ); // PreparedStatement is added to JDBCContext to close automatically
            String name = OliveUtils.mapToPrimitive( String.class, ps ); // The underlying ResultSet will be closed automatically
            Assert.assertEquals( name, "bob" );
        } );
    }
}
