package za.sabob.olive.jdbc.nested;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.config.OliveConfig;
import za.sabob.olive.postgres.PostgresBaseTest;
import za.sabob.olive.postgres.PostgresTestUtils;

public class NestedJDBCTest extends PostgresBaseTest {

    @BeforeClass( alwaysRun = true )
    public void beforeThisClass() {
        PostgresTestUtils.populateDatabase( ds );
    }

    @Test
    public void noNestedOperationTest() {
        OliveConfig.setAllowNestingOperations( false );

        JDBC.inOperation( ds, ( ctx ) -> {

            try {
                JDBC.inOperation( ds, ( ctx2 ) -> {

                } );

                Assert.fail( "Nested operations not allowed" );

            } catch ( Exception ex ) {
                ex.printStackTrace();
                // ignore
            }

        } );
    }

    @Test
    public void noNestedTransactionTest() {
        OliveConfig.setAllowNestingOperations( false );

        JDBC.inTransaction( ds, ( ctx ) -> {

            try {
                JDBC.inTransaction( ds, ( ctx2 ) -> {

                } );

                Assert.fail( "Nested transactions not allowed" );

            } catch ( Exception ex ) {
                ex.printStackTrace();
                // ignore
            }

        } );
    }

    @Test
    public void noNestedMixedTest() {
        OliveConfig.setAllowNestingOperations( false );

        JDBC.inOperation( ds, ( ctx ) -> {

            try {
                JDBC.inTransaction( ds, ( ctx2 ) -> {

                } );

                Assert.fail( "Nested operations and transactions are not allowed" );

            } catch ( Exception ex ) {
                ex.printStackTrace();
                // ignore
            }

        } );
    }
}
