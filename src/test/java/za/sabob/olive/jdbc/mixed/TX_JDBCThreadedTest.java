// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import java.sql.*;
import java.util.*;
import javax.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;

import static za.sabob.olive.util.DBTestUtils.isTimeout;

import za.sabob.olive.util.*;

public class TX_JDBCThreadedTest extends PostgresBaseTest {

    public TX_JDBCThreadedTest() {
        //JDBCConfig.setJoinableTransactionsDefault( false );
    }

    int personsCount = 0;

    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        System.out.println( "BEFORE CLASS" );
        ds = PostgresTestUtils.createDS( 20 );
        System.out.println( "Postgres created" );

        PostgresTestUtils.createPersonTable( ds );
    }

    @AfterClass( alwaysRun = true )
    public void afterClass() {
        super.afterClass();
        System.out.println( "AFTER CLASS" );
        Assert.assertEquals( personsCount, 400 );
    }

    @Test( successPercentage = 0, threadPoolSize = 20, invocationCount = 200, timeOut = 1110000 )
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginOperation( ds );
        } catch ( Throwable e ) {
            System.out.println( "CANNOT CREATE JDBCCONTEXT: " + e.getMessage() );
            e.printStackTrace();
        }
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            DBTestUtils.assertOpen( ctx );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

            nestedJDBC( ds );

            List<Person> persons = getJDBCPersons( ctx );

            personsCount = persons.size();

        } catch ( Throwable e ) {
            e.printStackTrace();
            System.out.println( "WHY 2? " + e.getMessage() );
            //throw new RuntimeException( e );

        } finally {

            DBTestUtils.assertOpen( ctx );
            DBTestUtils.assertOpen( ps );
            DBTestUtils.assertOpen( rs );

            JDBC.cleanupOperation( ctx );

            DBTestUtils.assertClosed( ctx );
            DBTestUtils.assertClosed( ps );
            DBTestUtils.assertClosed( rs );
        }
    }

    public void nestedJDBC( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );

        Assert.assertFalse( ctx.isClosed() );
        Assert.assertFalse( ctx.isConnectionClosed() );

        try {

            nestedTX( ds );

            List<Person> persons = getJDBCPersons( ctx );
            //System.out.println( "PERSONS " + persons.size() );

        } catch ( Throwable e ) {
            e.printStackTrace();
            System.out.println( "SERIOUS PROBLEM 1? " + e.getMessage() );

        } finally {

            try {

                Assert.assertFalse( ctx.isClosed() );
                Assert.assertFalse( ctx.isConnectionClosed() );

                JDBC.cleanupOperation( ctx );

                Assert.assertTrue( ctx.isClosed() );
                Assert.assertTrue( ctx.isConnectionClosed() );

            } catch ( Throwable e ) {
                e.printStackTrace();
            }
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            List<Person> persons = getTXPersons( ctx );

        } catch ( Exception ex ) {
            if ( isTimeout( ex ) ) {
                // ignore
            } else {
                ex.printStackTrace();
                throw new RuntimeException( ex );
            }
//            System.out.println( "SERIOUS PROBLEM 2" + throwable.getMessage() + ", fault? " + TX.isFaultRegisteringDS() + ", thread: "
//                + Thread.currentThread().getId() );

        } finally {

            try {

                Assert.assertFalse( ctx.isClosed() );
                Assert.assertFalse( ctx.isConnectionClosed() );

                JDBC.cleanupTransaction( ctx );

                Assert.assertTrue( ctx.isClosed() );
                Assert.assertTrue( ctx.isConnectionClosed() );

            } catch ( Exception e ) {
                System.out.println( "SERIOUS PROBLEM 2.2" + e.getMessage() );
                e.printStackTrace();
            }
        }
    }

    public List<Person> getPersons( Connection conn ) {

        try {
            PreparedStatement ps = OliveUtils.prepareStatement( conn, "select * from person" );

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

        Connection conn = ctx.getConnection();

        boolean isAutoCommit = OliveUtils.getAutoCommit( conn );
        Assert.assertTrue( isAutoCommit, " Connection should not be a transactional connection." );

        return getPersons( conn );

    }

    public List<Person> getTXPersons( JDBCContext ctx ) {
        Connection conn = ctx.getConnection();
        boolean isTransaction = !OliveUtils.getAutoCommit( conn );
        Assert.assertTrue( isTransaction, " Connection should be a transactional connection." );

        return getPersons( conn );

    }

    class Person {

        public long id;

        public String name;

    }

}
