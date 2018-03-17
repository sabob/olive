// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import za.sabob.olive.util.DBTestUtils;
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

public class TXCommitTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {

        ds = DBTestUtils.createDataSource( DBTestUtils.HSQLDB, 15 );
        DBTestUtils.createPersonTable( ds, DBTestUtils.HSQLDB );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        DBTestUtils.shutdown( ds );
    }

    @Test
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            conn = JDBC.beginOperation( ds );

            nestedJDBC( ds );

            List<Person> persons = getPersons();

            personsCount = persons.size();
            //Assert.assertEquals( personsCount, 0 );

        } catch ( Throwable e ) {
            throw new RuntimeException( e );

        } finally {

            boolean isAtRoot = JDBC.isAtRootConnection();
            Assert.assertTrue( isAtRoot );

            JDBC.cleanupOperation( ps, rs );

            isAtRoot = JDBC.isAtRootConnection();
            Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }
    }

    public void nestedJDBC( DataSource ds ) {

        Connection conn = null;

        try {

            conn = JDBC.beginOperation( ds );

            nestedTX( ds );

            List<Person> persons = getPersons();

        } catch ( Exception e ) {
            e.printStackTrace();

        } finally {
            //Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( conn );
            //Assert.assertTrue( JDBC.isAtRootConnection() );
        }
    }

    public void nestedTX( DataSource ds ) {

        Connection conn = null;
        PreparedStatement ps = null;
        Exception ex = null;

        try {

            conn = TX.beginTransaction( ds );
            //conn.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED);
            //System.out.println( "In Transaction? " + !conn.getAutoCommit() );
            //System.out.println( "Isolation level? " + OliveUtils.getTransactionIsolation( conn ) );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( conn, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();
            Assert.assertEquals( count, 1 );

            params.set( "name", "John" );
            //count = ps.executeUpdate();

            TX.rollbackTransaction();

            List<Person> persons = getPersons();

            Assert.assertEquals( persons.size(), 0 );
            Assert.assertEquals( count, 1 );

        } catch ( Exception error ) {
            ex = error;
            //throw new RuntimeException(error);

        } finally {
            //Assert.assertTrue( TX.isAtRootConnection() );
            RuntimeException result = TX.cleanupTransaction( ex, conn, ps );

            if ( result != null ) {
                throw result;
            }
            //Assert.assertFalse( TX.isAtRootConnection() );
        }
    }

    public List<Person> getPersons() {
        Connection conn = JDBCContext.getLatestConnection();
        return getPersons( conn );
    }

    public List<Person> getPersons( Connection conn ) {

        //Connection conn = JDBC.beginOperation( ds );
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

    }

    class Person {

        public long id;

        public String name;

    }

}
