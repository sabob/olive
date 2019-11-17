package za.sabob.olive.jdbc.postgres.single;

import java.sql.*;
import java.util.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.util.*;

public class SinglePGTransactionTest extends PostgresBaseTest {

    @Test
    public void commitTest() {
        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginTransaction( ds );

            OliveUtils.setTransactionIsolation( ctx.getConnection(), Connection.TRANSACTION_READ_COMMITTED );
            System.out.println( "Conn isolation level: " + ctx.getConnection().getTransactionIsolation() );

            insertPersons( ctx );

            List<Person> persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 0 );

            JDBC.commitTransaction( ctx );

            persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 2 );

        } catch ( Exception ex ) {

            throw JDBC.rollbackTransaction( ctx, ex );

        } finally {
            Assert.assertFalse( OliveUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertFalse( ctx.isClosed() );

            JDBC.cleanupTransaction( ctx );

            Assert.assertTrue( OliveUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertTrue( ctx.isClosed() );
        }
    }


}
