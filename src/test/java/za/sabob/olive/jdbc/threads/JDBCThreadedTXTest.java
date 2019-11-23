package za.sabob.olive.jdbc.threads;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.JDBCContext;
import za.sabob.olive.postgres.PostgresBaseTest;
import za.sabob.olive.postgres.PostgresTestUtils;
import za.sabob.olive.ps.SqlParams;
import za.sabob.olive.query.RowMapper;
import za.sabob.olive.util.OliveUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JDBCThreadedTXTest extends PostgresBaseTest {

    int personsCount = 0;

    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        ds = PostgresTestUtils.createDS( 20 );
        System.out.println( "Postgres created" );
        PostgresTestUtils.createPersonTable( ds );
        ds.setCheckoutTimeout( 2000 ); // There should be no deadlocks because Olive uses only 1 connection per thread.
    }

    @Test( successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000 )
    public void threadTest() {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "john" );
            count = ps.executeUpdate();

            nested( ds );

            List<Person> persons = getPersons( ctx );

            personsCount = persons.size();

        } catch ( Exception e ) {
            throw JDBC.rollbackTransaction( ctx, e );

        } finally {

            Assert.assertFalse( OliveUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertFalse( ctx.isClosed() );

            JDBC.cleanupTransaction( ctx );

            Assert.assertTrue( ctx.isClosed() );
        }
    }

    public void nested( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            List<Person> persons = getPersons( ctx );

            Assert.assertFalse( OliveUtils.getAutoCommit( ctx.getConnection() ) );
            Assert.assertTrue( ctx.isOpen() );

        } finally {
            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isClosed() );
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
