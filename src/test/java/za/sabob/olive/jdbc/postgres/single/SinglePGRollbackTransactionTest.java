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

public class SinglePGRollbackTransactionTest extends PostgresBaseTest {

    @Test
    public void rollbackTest() {
        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginTransaction( ds );

            OliveUtils.setTransactionIsolation( ctx.getConnection(), Connection.TRANSACTION_READ_COMMITTED );
            System.out.println( "Conn isolation level: " + ctx.getConnection().getTransactionIsolation() );

            insertPersons( ctx );

            List<Person> persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 0 );

            JDBC.rollbackTransaction( ctx );

            persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 0 );

        } catch ( Exception ex ) {

            throw JDBC.rollbackTransaction( ctx, ex );

        } finally {
            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot );

            JDBC.cleanupTransaction( ctx );

            isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot );
        }
    }


}
