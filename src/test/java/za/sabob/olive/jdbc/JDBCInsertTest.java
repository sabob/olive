package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.util.*;

public class JDBCInsertTest {

    DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource( DBTestUtils.H2 );
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        DBTestUtils.shutdown( ds );        
    }

    @Test
    public void basicTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
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

            nested( ds );

            List<Person> persons = getPersons( ctx );

            Assert.assertEquals( persons.size(), 2 );

        } catch ( SQLException e ) {
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

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            List<Person> persons = getPersons( ctx );
            Assert.assertEquals( persons.size(), 2 );

        } finally {
            Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( JDBC.isAtRootConnection() );
        }
    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person" );

        List<Person> persons = OliveUtils.mapToBeans(ps, new RowMapper<Person>() {
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
