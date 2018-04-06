package za.sabob.olive.jdbc2.postgres.single;

import java.sql.*;
import java.util.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.postgres.AbstractPGBaseTest;
import za.sabob.olive.util.*;

public class SinglePGTransactionTest extends AbstractPGBaseTest {

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
            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot );

            JDBC.cleanupTransaction( ctx );

            isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot );
        }
    }


}
