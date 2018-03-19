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

public class TX_JDBCTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        //ds = new JdbcDataSource();
        ds = DBTestUtils.createDataSource( DBTestUtils.H2, 5 );

        //ds.setURL( "jdbc:h2:~/test" );
        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        DBTestUtils.shutdown( ds );
        Assert.assertEquals( personsCount, 2 );
    }

    @Test
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx;
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

            List<Person> persons = getPersons( ctx );

            personsCount = persons.size();

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

    public void nestedJDBC( DataSource ds ) {

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            nestedTX( ds );

            List<Person> persons = getPersons( ctx );
            
        } catch (Exception e) {
            //e.printStackTrace();

        } finally {
            Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( JDBC.isAtRootConnection() );
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = null;

        try {

            ctx = TX.beginTransaction( ds );

            List<Person> persons = getPersons( ctx );

        } finally {
            boolean isAtRoot = TX.isAtRootConnection();
            Assert.assertTrue( isAtRoot );
            TX.cleanupTransaction( ctx );
            Assert.assertFalse( TX.isAtRootConnection() );
        }

    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person" );

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
