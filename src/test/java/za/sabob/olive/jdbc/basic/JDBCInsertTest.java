package za.sabob.olive.jdbc.basic;

import za.sabob.olive.jdbc.JDBCContext;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;
import za.sabob.olive.jdbc.ps.*;
import za.sabob.olive.query.*;

public class JDBCInsertTest extends PostgresBaseTest {

    @Test
    public void basicTest() throws SQLException {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = JDBC.beginOperation( ds );

        SqlParams params = new SqlParams();
        params.set( "name", "bob" );

        PreparedStatement ps = JDBCUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

        int count = ps.executeUpdate();

        params.set( "name", "john" );
        count = ps.executeUpdate();

        nested( ds );

        List<Person> persons = getPersons( ctx );

        Assert.assertEquals( persons.size(), 2 );

        Assert.assertFalse(  ctx.isClosed() );
        Assert.assertFalse(  ps.isClosed() );
        Assert.assertFalse( ctx.isConnectionClosed() );

        JDBC.cleanupOperation( ctx );

        Assert.assertTrue( ctx.isClosed() );
        Assert.assertTrue(  ps.isClosed() );
        Assert.assertTrue( ps.isClosed());
        Assert.assertTrue( ctx.isConnectionClosed() );
    }

    public void nested( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );

        List<Person> persons = getPersons( ctx );
        Assert.assertEquals( persons.size(), 2 );

    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = JDBCUtils.prepareStatement( ctx.getConnection(), "select * from person" );

        List<Person> persons = JDBCUtils.mapToList( ps, new RowMapper<Person>() {
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
