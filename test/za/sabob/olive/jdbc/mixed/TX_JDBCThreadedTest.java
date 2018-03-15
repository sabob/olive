// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import za.sabob.olive.jdbc.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.transaction.*;
import za.sabob.olive.util.*;

public class TX_JDBCThreadedTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource( 2 );

        //ds.setURL( "jdbc:h2:~/test" );
        DBTestUtils.createPersonTable( ds );
    }

    @AfterClass
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        //boolean success = ds.getConnection().createStatement().execute( "SHUTDOWN" );
        //System.out.println( "SHUTDOWN? " + success );
        //Assert.assertEquals( personsCount, 200 );
    }

    @Test(successPercentage = 100, threadPoolSize = 10, invocationCount = 100, timeOut = 1110000)
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            conn = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( conn, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

            nestedJDBC( ds );

            List<Person> persons = getPersons( conn );

            personsCount = persons.size();

        } catch ( SQLException e ) {
            //throw new RuntimeException( e );

        } finally {

            try {

                boolean isRoot = JDBC.isAtRootConnection();
                boolean connectionCreated = conn != null;

                if ( connectionCreated ) {
                    Assert.assertTrue( isRoot, "JDBC Connection was created, we must be root" );
                    //JDBC.isAtRootConnection();

                } else {
                    if ( isRoot ) {
                        System.out.println( "BUG JDBC 1, conn: " + connectionCreated + ", isRoot: " + isRoot );
                    }
                }

                //Assert.assertFalse(  );
                JDBC.cleanupOperation( conn );
                //Assert.assertTrue( JDBC.isAtRootConnection() );
                isRoot = JDBC.isAtRootConnection();
                if ( isRoot ) {
                    System.out.println( "BUG 2" );
                }

            } catch ( Exception e ) {
                System.out.println( "WHY: " + e.getMessage() );
            }

            //Assert.assertTrue( isAtRoot );
            //Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }
    }

    public void nestedJDBC( DataSource ds ) {

        Connection conn = null;

        try {

            conn = JDBC.beginOperation( ds );

            nestedTX( ds );

            List<Person> persons = getPersons( conn );
            System.out.println( "PERSONS " + persons.size() );

        } catch ( Exception e ) {
            //System.out.println( "Whats this? " + e.getMessage() );

        } finally {
            try {

                boolean isRoot = JDBC.isAtRootConnection();
                if ( isRoot ) {
                    System.out.println( "BUG 1.1" );
                }
                //Assert.assertFalse(  );
                JDBC.cleanupOperation( conn );
                //Assert.assertTrue( JDBC.isAtRootConnection() );
                isRoot = JDBC.isAtRootConnection();
                if ( !isRoot ) {
                    System.out.println( "BUG 1.2" );
                }
            } catch ( Exception e ) {
                System.out.println( "SERIOUS PROBLEM" );
            }
        }

    }

    public void nestedTX( DataSource ds ) {

        Connection conn = null;

        try {

            conn = TX.beginTransaction( ds );

            List<Person> persons = getPersons( conn );

        } catch ( Throwable throwable ) {
            System.out.println( "ERR: " + throwable.getMessage() );

        } finally {
            boolean isRoot = TX.isAtRootConnection();
            boolean connectionCreated = conn != null;

            if ( connectionCreated ) {
                Assert.assertTrue( isRoot, "TX Connection was created, we must be root" );

            } else {
                if ( isRoot ) {
                    System.out.println( "BUG TX, conn creted?: " + connectionCreated + ", isRoot: " + isRoot );
                }
            }
        }
    }

    public List<Person> getPersons( Connection conn ) {
        try {

            PreparedStatement ps = OliveUtils.prepareStatement( conn, "select * from person" );

            List<Person> persons = OliveUtils.query( ps, new RowMapper<Person>() {
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

    class Person {

        public long id;

        public String name;

    }

}
