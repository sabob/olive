// TODO mix jdbc and tx test
package za.sabob.olive.jdbc2.mixed;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import static za.sabob.olive.util.DBTestUtils.isTimeout;
import za.sabob.olive.util.*;

public class TX_JDBCThreadedTest extends PostgresBaseTest {

    int personsCount = 0;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = PostgresTestUtils.createDS( 20 );
        System.out.println( "Postgres created" );

        PostgresTestUtils.createPersonTable( ds );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        super.afterClass();
        Assert.assertEquals( personsCount, 400 );
    }

    //@Test(successPercentage = 100, threadPoolSize = 3, invocationCount = 20, timeOut = 1110000)
    @Test(successPercentage = 0, threadPoolSize = 20, invocationCount = 200, timeOut = 1110000)
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = null;
        ResultSet rs = null;

        try {

            boolean isEmpty = DSF.getDataSourceContainer().isEmpty();
            Assert.assertTrue( isEmpty, "CLEAN: " );

            ctx = JDBC.beginOperation( ds );

            isEmpty = ctx.isRootConnectionHolder();
            Assert.assertTrue( isEmpty, "CLEAN:" );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

            nestedJDBC( ds );

            List<Person> persons = getJDBCPersons();

            personsCount = persons.size();

        } catch ( Throwable e ) {
            System.out.println( "WHY 2? " + e.getMessage() );
            //throw new RuntimeException( e );

        } finally {

            boolean isRoot = ctx.isRootContext();

            Assert.assertTrue( isRoot, "JDBC Connection was created, we must be root" );

            boolean isEmpty = DSF.getDataSourceContainer().isEmpty();
            Assert.assertFalse( isEmpty );

            JDBC.cleanupOperation( ctx );
            isEmpty = DSF.getDataSourceContainer().isEmpty();
            Assert.assertTrue( isEmpty );
        }
    }

    public void nestedJDBC( DataSource ds ) {

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            nestedTX( ds );

            List<Person> persons = getJDBCPersons();
            //System.out.println( "PERSONS " + persons.size() );

        } catch ( Throwable e ) {
            System.out.println( "SERIOUS PROBLEM 1? " + e.getMessage() );

        } finally {

            boolean isRoot = ctx.isRootConnectionHolder();
            boolean connectionCreated = ctx != null;

            if ( connectionCreated ) {
                //Assert.assertTrue( isRoot, "JDBC Connection was created, we must be root" );
                Assert.assertFalse( isRoot, "2nd JDBC Connection was created, we must NOT be root" );
                //JDBC.isAtRootConnection();

            } else {
                if ( isRoot ) {
                    System.out.println( "BUG JDBC 1.1, conn: " + connectionCreated + ", isRoot: " + isRoot );
                }
            }

            //Assert.assertFalse(  );
            JDBC.cleanupOperation( ctx );
            //Assert.assertTrue( JDBC.isAtRootConnection() );
            isRoot = ctx.isRootConnectionHolder();
            if ( !isRoot ) {
                System.out.println( "BUG 1.2" );
            }

        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginTransaction( ds );

            List<Person> persons = getTXPersons();

        } catch ( Exception ex ) {
            if ( isTimeout( ex ) ) {
                // ignore
            } else {
                throw new RuntimeException( ex );
            }
//            System.out.println( "SERIOUS PROBLEM 2" + throwable.getMessage() + ", fault? " + TX.isFaultRegisteringDS() + ", thread: "
//                + Thread.currentThread().getId() );

        } finally {

            try {

                boolean isRoot = ctx.isRootConnectionHolder();
                boolean connectionCreated = ctx != null;

                JDBC.cleanupTransaction( ctx );

                if ( connectionCreated ) {
                    Assert.assertTrue( isRoot, "TX Connection was created, we must be root" );

                } else {
                    if ( isRoot ) {
                        System.out.println( "BUG TX, conn creted?: " + connectionCreated + ", isRoot: " + isRoot );
                    }
                }
            } catch ( Exception e ) {
                System.out.println( "SERIOUS PROBLEM 2.2" + e.getMessage() );
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

    public List<Person> getJDBCPersons() {
        JDBCContext ctx = DSF.getLatestJDBCContext( ds );
        Connection conn = ctx.getConnection();

        boolean isAutoCommit = OliveUtils.getAutoCommit( conn );
        Assert.assertTrue( isAutoCommit, " Connection should not be a transactional connection." );

        return getPersons( conn );

    }

    public List<Person> getTXPersons() {
        JDBCContext ctx = DSF.getLatestJDBCContext( ds );
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
