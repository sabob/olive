package za.sabob.olive.jdbc2.postgres.multyiple;

import java.util.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.postgres.*;

public class MultiPGRollbackTransactionTest extends AbstractPGBaseTest {

    @Test
    public void rollbackTest() {

        JDBCContext parent = null;
            JDBCContext child = null;

            try {

                parent = JDBC.beginTransaction( ds );
                child = JDBC.beginTransaction( ds );

                //OliveUtils.setTransactionIsolation( parent.getConnection(), Connection.TRANSACTION_READ_COMMITTED );
                insertPersons( child );

                List<Person> persons = getPersons( ds );
                Assert.assertEquals( persons.size(), 0 );

                JDBC.rollbackTransaction( child ); // child won't rollback

                persons = getPersons( ds );
                Assert.assertEquals( persons.size(), 0 );

                JDBC.commitTransaction( parent );

                persons = getPersons( ds );
                Assert.assertEquals( persons.size(), 2 );

            } catch ( Exception ex ) {

                throw JDBC.rollbackTransaction( parent, ex );

            } finally {
                boolean isAtRoot = parent.isRootContext();
                Assert.assertTrue( isAtRoot );

                JDBC.cleanupTransaction( parent );

                isAtRoot = parent.isRootContext();
                Assert.assertTrue( isAtRoot );
            }
        }
    }
