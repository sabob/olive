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

public class TXCommitTest extends PostgresBaseTest {

    int personsCount = 0;

    @Test
    public void threadTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = JDBC.beginOperation( ds );

        try {

            nestedJDBC( ds );

            List<Person> persons = getPersons();

            personsCount = persons.size();
            //Assert.assertEquals( personsCount, 0 );

        } catch ( Throwable e ) {
            throw new RuntimeException( e );

        } finally {

            boolean isAtRoot = ctx.isRootConnectionHolder();
            Assert.assertTrue( isAtRoot );

            JDBC.cleanupOperation( ctx );

            isAtRoot = ctx.isRootConnectionHolder();
            Assert.assertTrue( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
            Assert.assertTrue( DSF.getDataSourceContainer().isEmpty() );

        }
    }

    public void nestedJDBC( DataSource ds ) {

        JDBCContext ctx = JDBC.beginOperation( ds );

        try {

            nestedTX( ds );

            List<Person> persons = getPersons();

        } catch ( Exception e ) {
            e.printStackTrace();

        } finally {
            //Assert.assertFalse( JDBC.isAtRootConnection() );
            JDBC.cleanupOperation( ctx );
            //Assert.assertTrue( JDBC.isAtRootConnection() );
        }
    }

    public void nestedTX( DataSource ds ) {

        JDBCContext ctx = JDBC.beginTransaction( ds );
        Exception ex = null;

        try {

            //conn.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED);
            //System.out.println( "In Transaction? " + !conn.getAutoCommit() );
            //System.out.println( "Isolation level? " + OliveUtils.getTransactionIsolation( conn ) );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();
            Assert.assertEquals( count, 1 );

            params.set( "name", "John" );
            //count = ps.executeUpdate();

            JDBC.rollbackTransaction( ctx );

            List<Person> persons = getPersons();

            Assert.assertEquals( persons.size(), 0 );
            Assert.assertEquals( count, 1 );

        } catch ( Exception error ) {
            ex = error;
            //throw new RuntimeException(error);

        } finally {
            //Assert.assertTrue( TX.isAtRootConnection() );
            RuntimeException result = JDBC.cleanupTransactionQuietly( ctx, ex );

            if ( result != null ) {
                throw result;
            }
            //Assert.assertFalse( TX.isAtRootConnection() );
        }
    }

    public List<Person> getPersons() {
        JDBCContext latestCtx = DSF.getLatestJDBCContext( ds );
        Connection conn = latestCtx.getConnection();
        return getPersons( conn );
    }

    public List<Person> getPersons( Connection conn ) {

        //Connection conn = JDBC.beginOperation( ds );
        PreparedStatement ps = OliveUtils.prepareStatement( conn, "select * from person" );

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
