package za.sabob.olive.jdbc2.postgres;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class AbstractPGBaseTest {


    public DataSource ds;

    @BeforeClass
    public void beforeClass() {
        boolean multiThreaded = false;
        int poolSize = 10;
        ds = DBTestUtils.createDataSource( DBTestUtils.POSTGRES, poolSize, multiThreaded );
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.POSTGRES );

        DBTestUtils.clearPersonTable( ds );

    }

    @AfterClass
    public void afterClass() {
        DBTestUtils.clearPersonTable( ds );
    }


    public void insertPersons( JDBCContext ctx ) {
        try {
            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );

            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            int count;
            count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public List<Person> getPersons( DataSource ds ) {
        //public List<Person> getPersons( JDBCContext ctx ) {
        JDBCContext ctx = JDBC.beginOperation( ds );
        OliveUtils.setTransactionIsolation( ctx.getConnection(), Connection.TRANSACTION_READ_COMMITTED );

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

    public class Person {

        public long id;

        public String name;

    }
}
