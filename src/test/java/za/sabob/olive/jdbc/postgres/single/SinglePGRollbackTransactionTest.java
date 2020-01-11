package za.sabob.olive.jdbc.postgres.single;

import java.sql.*;
import java.util.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;
import za.sabob.olive.util.*;

public class SinglePGRollbackTransactionTest extends PostgresBaseTest {

    @Test
    public void rollbackTest() {
        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            JDBCUtils.setTransactionIsolation( ctx.getConnection(), Connection.TRANSACTION_READ_COMMITTED );
            System.out.println( "Conn isolation level: " + ctx.getConnection().getTransactionIsolation() + ", autocommit: "
                + ctx.getConnection().getAutoCommit() );

            insertPersons( ctx );

            List<Person> persons = getPersons( ds );
            System.out.println( "Conn isolation level: " + ctx.getConnection().getTransactionIsolation() + ", autocommit: "
                + ctx.getConnection().getAutoCommit() );
            Assert.assertEquals( persons.size(), 0 );

            JDBC.rollbackTransaction( ctx );

            persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 0 );

        } catch ( Exception ex ) {

            JDBC.rollbackTransactionAndThrow( ctx, ex );
        } finally {

            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isConnectionClosed());
            Assert.assertTrue( ctx.isClosed());
        }
    }


}
