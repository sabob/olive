package za.sabob.olive.jdbc.postgres.multiple;

import java.util.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;
import za.sabob.olive.util.OliveUtils;

public class MultiPGTransactionTest extends PostgresBaseTest {

    @Test
    public void commitTest() {
        JDBCContext parent = null;
        JDBCContext child = null;

        try {

            parent = JDBC.beginTransaction( ds );
            child = JDBC.beginTransaction( ds );

            //OliveUtils.setTransactionIsolation( parent.getConnection(), Connection.TRANSACTION_READ_COMMITTED );

            insertPersons( child );

            List<Person> persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 0 );

            JDBC.commitTransaction( child ); // child won't commit

            persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 0 );

            JDBC.commitTransaction( parent );

            persons = getPersons( ds );
            Assert.assertEquals( persons.size(), 2 );

        } catch ( Exception ex ) {

            JDBC.rollbackTransactionAndThrow( parent, ex );

        } finally {
            Assert.assertTrue( parent.isOpen() );

            JDBC.cleanupTransaction( parent );

            Assert.assertTrue( parent.isClosed() );
            Assert.assertFalse( JDBCUtils.getAutoCommit( parent.getConnection() ) );
        }
    }


}
