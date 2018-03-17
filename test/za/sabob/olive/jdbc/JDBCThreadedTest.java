package za.sabob.olive.jdbc;

import za.sabob.olive.util.DBTestUtils;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class JDBCThreadedTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource( DBTestUtils.H2 );
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );

        try {
            Connection conn = JDBC.beginOperation( ds );
            List<Person> persons = getPersons( conn );
            //System.out.println( "INITIAL PERSONS "  + persons.size() );

        } catch ( Exception e ) {
        }

    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {

        //ds = DBTestUtils.createDataSource();
        ds.getConnection().createStatement().execute( "SHUTDOWN" );
        Assert.assertEquals( personsCount, 200 );
    }

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        Connection conn;
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

            nested( ds );

            List<Person> persons = getPersons( conn );

            personsCount = persons.size();
            //System.out.println( "PERSONS: " + personsCount );

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

    public void nested( DataSource ds ) {

        Connection conn = null;

        try {

            conn = JDBC.beginOperation( ds );

            List<Person> persons = getPersons( conn );

        } finally {
            Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( conn );
            Assert.assertTrue( JDBC.isAtRootConnection() );
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
