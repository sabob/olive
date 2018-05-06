// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import za.sabob.olive.jdbc.context.JDBCContext;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.DSF;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.hsqldb.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import static za.sabob.olive.util.DBTestUtils.isTimeout;
import za.sabob.olive.util.*;

public class ForceTimeoutTest {

    int personsCount = 0;

    private DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = HSQLDBTestUtils.createDS( 5 );
        System.out.println( "HSQLDB created" );
        HSQLDBTestUtils.createPersonTable( ds );
    }

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
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

            List<Person> persons = getJDBCPersons();

            personsCount = persons.size();

        } catch ( Throwable e ) {
            if ( isTimeout( e ) ) {
                // ignore
            } else {
                throw new RuntimeException( e );
            }

            //System.out.println( "WHY 2? " + e.getMessage() );
            //throw new RuntimeException( e );
        } finally {

            try {

                boolean connectionCreated = ctx != null;

                if ( connectionCreated ) {

                    boolean isRoot = ctx.isRootContext();

                    Assert.assertTrue( isRoot, "JDBC Connection was created, we must be root" );

                    JDBC.cleanupOperation( ctx );
                    Assert.assertTrue( ctx.isRootContext() );
                    Assert.assertTrue( ctx.isRootConnectionHolder() );
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
        ctx.isRootContext();

        try {

            nestedTX( ds );

            List<Person> persons = getJDBCPersons();
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

                boolean isRoot = ctx.isRootContext();

                //Assert.assertFalse(  );
                JDBC.cleanupOperation( ctx );
                //Assert.assertTrue( JDBC.isAtRootConnection() );
                isRoot = ctx.isRootContext();

                if ( !isRoot ) {
                    System.out.println( "BUG 1.2" );
                }
            } catch ( Throwable e ) {
                System.out.println( "SERIOUS PROBLEM 1.1" + e.getMessage() );
                JDBC.cleanupOperation( ctx );
            }
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            List<Person> persons = getTXPersons();

        } catch ( Exception ex ) {

            if ( isTimeout( ex ) ) {
                //ignore
            } else {
                throw new RuntimeException( ex );

            }
//            err = ex;
//            System.out.println( "SERIOUS PROBLEM 2" + ex.getMessage() + ", fault? " + TX.isFaultRegisteringDS() + ", thread: "
//                + Thread.currentThread().getId() );

        } finally {

            try {

//                if ( err != null ) {
//                    System.out.println( "..." );
//                }
                boolean isRoot = ctx.isRootConnectionHolder();
                boolean connectionCreated = ctx != null;

                JDBC.cleanupTransaction( ctx );

                isRoot = ctx.isRootContext();
                Assert.assertTrue( isRoot, "TX Connection was closed, this must be root Context" );

                if ( connectionCreated ) {
                    Assert.assertTrue( isRoot, "TX Connection was created, we must be root connection holder " );

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

    public List<Person> getJDBCPersons() {
        JDBCContext ctx = DSF.getLatestJDBCContext( ds );
        boolean isAutoCommit = OliveUtils.getAutoCommit( ctx.getConnection() );
        Assert.assertTrue( isAutoCommit, " Connection should not be a transactional connection." );

        return getPersons( ctx );

    }

    public List<Person> getTXPersons() {
        JDBCContext ctx = DSF.getLatestJDBCContext( ds );
        boolean isTransaction = !OliveUtils.getAutoCommit( ctx.getConnection() );
        Assert.assertTrue( isTransaction, " Connection should be a transactional connection." );

        return getPersons( ctx );

    }
}
