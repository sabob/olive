package za.sabob.olive.jdbc2;

import za.sabob.olive.jdbc2.context.JDBCContext;
import za.sabob.olive.util.DBTestUtils;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
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
    public void basicTest() throws SQLException {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = JDBC.beginOperation( ds );

        SqlParams params = new SqlParams();
        params.set( "name", "Bob" );

        PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

        int count = ps.executeUpdate();

        params.set( "name", "John" );
        count = ps.executeUpdate();

        nested( ds );

        List<Person> persons = getPersons( ctx );

        Assert.assertEquals( persons.size(), 2 );

        Assert.assertTrue( ctx.isRootContext() );

        JDBC.cleanupOperation( ctx );

        Assert.assertTrue( ctx.isClosed() );
        Assert.assertTrue( ps.isClosed());
        Assert.assertTrue( ctx.getConnection().isClosed() );
    }

    public void nested( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );

        List<Person> persons = getPersons( ctx );
        Assert.assertEquals( persons.size(), 2 );

    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person" );

        List<Person> persons = OliveUtils.mapToList( ps, new RowMapper<Person>() {
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
