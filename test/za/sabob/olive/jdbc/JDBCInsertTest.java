package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.h2.jdbcx.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class JDBCInsertTest {

    DataSource ds;

    @BeforeClass
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource();
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds );
    }

    @AfterClass
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = DBTestUtils.createDataSource();
    }

    @Test
    public void basicTest() {
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

            Assert.assertEquals( persons.size(), 2 );

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
boolean isAtRoot = JDBC.isAtRootConnection();
            Assert.assertTrue( isAtRoot );

            JDBC.cleanupOperation(ps, rs );

            isAtRoot = JDBC.isAtRootConnection();
            Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation");
        }
    }

    public void nested( DataSource ds ) {
        
        Connection conn = null;

        try {

            conn = JDBC.beginOperation( ds );

            List<Person> persons = getPersons( conn );
            Assert.assertEquals( persons.size(), 2 );

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
