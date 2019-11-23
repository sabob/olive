// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import java.sql.*;
import java.util.*;
import javax.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;

import static za.sabob.olive.util.DBTestUtils.isTimeout;

import za.sabob.olive.util.*;

public class DeadlockTest extends PostgresBaseTest {

    int personsCount = 0;


    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        ds = PostgresTestUtils.createDS( 1 );
        System.out.println( "Postgres created" );
        PostgresTestUtils.createPersonTable( ds );
        ds.setCheckoutTimeout( 0 ); // There should be no deadlocks because Olive uses only 1 connection per thread.
    }

    @Test( successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000 )
    public void threadTest() throws Exception {

        JDBCContext ctx = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            ctx = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

            nestedJDBC( ds );

            List<Person> persons = getJDBCPersons( ctx );

            personsCount = persons.size();

        } catch ( Throwable e ) {
            if ( isTimeout( e ) ) {
                throw new IllegalStateException( "Oh no! Timeout!! ", e );
            } else {
                throw new RuntimeException( e );
            }

            //System.out.println( "WHY 2? " + e.getMessage() );
            //throw new RuntimeException( e );
        } finally {

            try {

                boolean connectionCreated = ctx != null;

                if ( connectionCreated ) {

                    Assert.assertFalse( ctx.isConnectionClosed() );

                    JDBC.cleanupOperation( ctx );

                    Assert.assertTrue( ctx.isConnectionClosed() );
                }

            } catch ( Throwable e ) {
                e.printStackTrace();
                System.out.println( "WHY: " + e.getMessage() );
                JDBC.cleanupOperation( ctx );
            }

            //Assert.assertTrue( isAtRoot );
            //Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }
    }

    public void nestedJDBC( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );
        Assert.assertFalse( ctx.isConnectionClosed() );

        try {

            nestedTX( ds );

            List<Person> persons = getJDBCPersons( ctx );
            //System.out.println( "PERSONS " + persons.size() );

        } catch ( Throwable e ) {

            if ( isTimeout( e ) ) {
                // ignore
            } else {
                throw new RuntimeException( e );
            }
            //System.out.println( "SERIOUS PROBLEM 1? " + e.getMessage() );

        } finally {

            try {

                JDBC.cleanupOperation( ctx );
                Assert.assertTrue( ctx.isConnectionClosed() );

            } catch ( Throwable e ) {
                JDBC.cleanupOperation( ctx );
                Assert.assertTrue( ctx.isConnectionClosed() );
            }
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            List<Person> persons = getTXPersons( ctx );

        } catch ( Exception ex ) {

            if ( isTimeout( ex ) ) {
                //ignore
            } else {
                throw new RuntimeException( ex );

            }

        } finally {

            try {

                JDBC.cleanupTransaction( ctx );
                Assert.assertTrue( ctx.isConnectionClosed() );

            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }

    public List<Person> getPersons( JDBCContext ctx ) {

        try {
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "select * from person" );

            List<Person> persons = OliveUtils.mapToBeans( ps, new RowMapper<Person>() {
                @Override
                public Person map( ResultSet rs, int rowNum ) throws SQLException {
                    Person person = new Person();
                    person.id = rs.getLong( "id" );
                    person.name = rs.getString( "name" );
                    return person;
                }
            } );

            return persons;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public List<Person> getJDBCPersons( JDBCContext ctx ) {

        boolean isAutoCommit = OliveUtils.getAutoCommit( ctx.getConnection() );
        Assert.assertTrue( isAutoCommit, " Connection should not be a transactional connection." );

        return getPersons( ctx );

    }

    public List<Person> getTXPersons( JDBCContext ctx ) {
        boolean isTransaction = !OliveUtils.getAutoCommit( ctx.getConnection() );
        Assert.assertTrue( isTransaction, " Connection should be a transactional connection." );

        return getPersons( ctx );

    }
}
