// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.transaction.*;
import static za.sabob.olive.util.DBTestUtils.isTimeout;
import za.sabob.olive.util.*;

public class ForceTimeoutTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource( DBTestUtils.H2, 5 ); // Use small pool so that retrieving connections from large amount of threads leads to deadlocks
        // that are resolved only through connection timeouts.

        //ds.setURL( "jdbc:h2:~/test" );
        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        DBTestUtils.shutdown( ds );
    }

    @Test(successPercentage = 0, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            boolean isRoot = JDBC.isAtRootConnection();
            Assert.assertFalse( isRoot, "CLEAN: " );

            ctx = JDBC.beginOperation( ds );
            isRoot = JDBC.isAtRootConnection();
            Assert.assertTrue( isRoot, "CLEAN:" );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

            nestedJDBC( ds );

            List<Person> persons = getJDBCPersons();

            personsCount = persons.size();

        } catch ( Exception e ) {
            if ( isTimeout( e )) {
                // ignore
            } else {
                throw new RuntimeException(e);
            }

            //System.out.println( "WHY 2? " + e.getMessage() );
            //throw new RuntimeException( e );

        } finally {

            try {

                boolean isRoot = JDBC.isAtRootConnection();
                boolean connectionCreated = ctx != null;

                if ( connectionCreated ) {

                    Assert.assertTrue( isRoot, "JDBC Connection was created, we must be root" );

                } else {
                    if ( isRoot ) {
                        System.out.println( "BUG JDBC 1, conn: " + connectionCreated + ", isRoot: " + isRoot );
                    }
                }

                //Assert.assertFalse(  );
                JDBC.cleanupOperation();
                //Assert.assertTrue( JDBC.isAtRootConnection() );
                isRoot = JDBC.isAtRootConnection();
                if ( isRoot ) {
                    System.out.println( "BUG 2" );
                }

            } catch ( Throwable e ) {
                System.out.println( "WHY: " + e.getMessage() );
                JDBC.cleanupOperation();
            }

            //Assert.assertTrue( isAtRoot );
            //Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
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

            try {

                boolean isRoot = JDBC.isAtRootConnection();
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
                JDBC.cleanupOperation();
                //Assert.assertTrue( JDBC.isAtRootConnection() );
                isRoot = JDBC.isAtRootConnection();
                if ( !isRoot ) {
                    System.out.println( "BUG 1.2" );
                }
            } catch ( Throwable e ) {
                System.out.println( "SERIOUS PROBLEM 1.1" + e.getMessage() );
                JDBC.cleanupOperation();
            }
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext conn = null;
        //Throwable err = null;

        try {

            conn = TX.beginTransaction( ds );

            List<Person> persons = getTXPersons();

        } catch ( Exception ex ) {

            if ( isTimeout( ex )) {
                //ignore
            } else {
                throw new RuntimeException(ex);

            }
//            err = ex;
//            System.out.println( "SERIOUS PROBLEM 2" + ex.getMessage() + ", fault? " + TX.isFaultRegisteringDS() + ", thread: "
//                + Thread.currentThread().getId() );

        } finally {

            try {

//                if ( err != null ) {
//                    System.out.println( "..." );
//                }

                boolean isRoot = TX.isAtRootConnection();
                boolean connectionCreated = conn != null;

                TX.cleanupTransaction( );

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
        Connection conn = JDBCLookup.getLatestConnection( ds );
            boolean isAutoCommit = OliveUtils.getAutoCommit( conn );
            Assert.assertTrue( isAutoCommit, " Connection should not be a transactional connection." );

        return getPersons( conn );

    }

    public List<Person> getTXPersons() {
        Connection conn = JDBCLookup.getLatestConnection( ds );
            boolean isTransaction = !OliveUtils.getAutoCommit( conn );
            Assert.assertTrue( isTransaction, " Connection should be a transactional connection." );

        return getPersons( conn );

    }

    public class Person {

        public long id;

        public String name;

    }

}
