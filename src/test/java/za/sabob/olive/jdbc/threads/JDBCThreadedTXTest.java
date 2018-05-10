package za.sabob.olive.jdbc.threads;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.config.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class JDBCThreadedTXTest extends PostgresBaseTest {

    int personsCount = 0;

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void threadTest() {
        JDBCConfig.setJoinableTransactionsDefault( true );

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

            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot );

            JDBC.cleanupTransaction( ctx );

            isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot );
        }
    }

    public void nested( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );

        try {

            List<Person> persons = getPersons( ctx );

        } finally {
            Assert.assertFalse( ctx.isRootContext() );
            JDBC.cleanupTransaction( ctx );
            Assert.assertTrue( ctx.isRootContext());
        }

    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = OliveUtils.prepareStatement( ctx, "select * from person" );

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
