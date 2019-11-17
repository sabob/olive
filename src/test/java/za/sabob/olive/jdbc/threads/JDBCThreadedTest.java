package za.sabob.olive.jdbc.threads;

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

public class JDBCThreadedTest {

    DataSource ds;

    int personsCount = 0;

    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        //ds = new JdbcDataSource();
        //ds = DBTestUtils.createDataSource( DBTestUtils.H2 );
        ds = PostgresTestUtils.createDS();
        PostgresTestUtils.createPersonTable( ds );
        //ds.setURL( "jdbc:h2:~/test" );

        //DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass( alwaysRun = true )
    public void afterClass() throws Exception {

        //ds = DBTestUtils.createDataSource();
        PostgresTestUtils.shutdown( ds );
        //Assert.assertEquals( personsCount, 200 );
    }

    @Test( successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000 )
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();

            Assert.assertTrue( OliveUtils.getAutoCommit( ctx.getConnection() ) );

            params.set( "name", "bob" );
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "john" );
            count = ps.executeUpdate();

            nested( ds );

            List<Person> persons = getPersons( ctx );

            personsCount = persons.size();
            //System.out.println( "PERSONS: " + personsCount );

        } catch ( Throwable e ) {
            throw new RuntimeException( e );

        } finally {

            JDBC.cleanupOperation( ctx );

            Assert.assertFalse( OliveUtils.getAutoCommit( ctx.getConnection() ) );
        }
    }

    public void nested( DataSource ds ) {

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            List<Person> persons = getPersons( ctx );

        } finally {
            Assert.assertTrue( ctx.isOpen() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isConnectionClosed() );
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
