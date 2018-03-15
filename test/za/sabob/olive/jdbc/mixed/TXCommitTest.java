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

public class TXCommitTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource( 5 );

        //ds.setURL( "jdbc:h2:~/test" );
        DBTestUtils.createPersonTable( ds );
    }

    @AfterClass
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        boolean success = ds.getConnection().createStatement().execute( "SHUTDOWN" );
        System.out.println( "SHUTDOWN? " + success );
        Assert.assertEquals( personsCount, 2 );
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

            List<Person> persons = getPersons( conn );

            personsCount = persons.size();

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

            List<Person> persons = getPersons( conn );

        } catch ( Exception e ) {
            e.printStackTrace();

        } finally {
            Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( conn );
            Assert.assertTrue( JDBC.isAtRootConnection() );
        }

    }

    public void nestedTX( DataSource ds ) {

        Connection conn = null;
        PreparedStatement ps = null;

        try {

            conn = TX.beginTransaction( ds );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( conn, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();
            
            TX.commitTransaction();
            
        } catch (Throwable error ) {
            throw new RuntimeException(error);

        } finally {
            Assert.assertTrue( TX.isAtRootConnection() );
            TX.cleanupTransaction( conn, ps );
            Assert.assertFalse( TX.isAtRootConnection() );
        }

    }

    public List<Person> getPersons( Connection conn ) {

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
