package za.sabob.olive.jdbc.threads;

import java.sql.*;
import java.util.*;
import javax.sql.*;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;
import za.sabob.olive.jdbc.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class JDBCThreadedTest {

    ComboPooledDataSource ds;

    int personsCount = 0;

    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        ds = PostgresTestUtils.createDS( 20 );
        System.out.println( "Postgres created" );
        PostgresTestUtils.createPersonTable( ds );
        ds.setCheckoutTimeout( 2000 ); // There should be no deadlocks because Olive uses only 1 connection per thread.
    }

    @AfterClass( alwaysRun = true )
    public void afterClass() throws Exception {

        //ds = DBTestUtils.createDataSource();
        PostgresTestUtils.shutdown( ds );
        //Assert.assertEquals( personsCount, 200 );
    }

    @Test( successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000 )
    public void threadTest() {
        //Connection conn = JDBCUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();

            Assert.assertTrue( JDBCUtils.getAutoCommit( ctx.getConnection() ) );

            params.set( "name", "bob" );
            PreparedStatement ps = JDBCUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "john" );
            count = ps.executeUpdate();

            nested( ds );

            List<Person> persons = getPersons( ctx );

            personsCount = persons.size();

            Assert.assertTrue( JDBCUtils.getAutoCommit( ctx.getConnection() ) );

        } catch ( Throwable e ) {
            throw new RuntimeException( e );

        } finally {

            JDBC.cleanupOperation( ctx );

            if ( ctx != null ) {
                Assert.assertTrue( ctx.isClosed() );
            }


        }
    }

    public void nested( DataSource ds ) {

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginOperation( ds );

            List<Person> persons = getPersons( ctx );

        } finally {

            JDBC.cleanupOperation( ctx );

            if ( ctx != null ) {
                Assert.assertTrue( ctx.isConnectionClosed() );
                Assert.assertTrue( ctx.isClosed() );

            }
        }

    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = JDBCUtils.prepareStatement( ctx, "select * from person" );

        List<Person> persons = JDBCUtils.mapToBeans( ps, new RowMapper<Person>() {
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
