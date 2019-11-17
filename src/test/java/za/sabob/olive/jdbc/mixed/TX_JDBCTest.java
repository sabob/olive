// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed;

import java.sql.*;
import java.util.*;
import javax.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class TX_JDBCTest extends PostgresBaseTest {

    int personsCount = 0;

    @AfterClass( alwaysRun = true )
    public void afterClass() {

        super.afterClass();
        Assert.assertEquals( personsCount, 2 );
    }

    @Test
    public void threadTest() {

        JDBCContext ctx = JDBC.beginOperation( ds );
        PreparedStatement ps = null;

        try {

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

            nestedJDBC( ds );

            List<Person> persons = getPersons( ctx );

            personsCount = persons.size();

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {

            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( OliveUtils.isClosed( ps ) );
            Assert.assertTrue( ctx.isConnectionClosed() );

        }
    }

    public void nestedJDBC( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );

        try {

            nestedTX( ds );

            List<Person> persons = getPersons( ctx );

        } catch ( Exception e ) {
            //e.printStackTrace();

        } finally {
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertTrue( ctx.isConnectionClosed() );
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginTransaction( ds );

            List<Person> persons = getPersons( ctx );

        } finally {
            Assert.assertFalse( ctx.isConnectionClosed() );
            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isClosed() );
            Assert.assertFalse( ctx.isConnectionClosed() );
        }

    }

    public List<Person> getPersons( JDBCContext ctx ) {

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

    }

    class Person {

        public long id;

        public String name;

    }

}
