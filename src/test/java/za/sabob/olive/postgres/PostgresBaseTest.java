package za.sabob.olive.postgres;

import com.mchange.v2.c3p0.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class PostgresBaseTest {

    protected ComboPooledDataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = PostgresTestUtils.createDS();
        System.out.println( "Postgres created" );

        PostgresTestUtils.createPersonTable( ds );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        System.out.println( "Postgres stopped" );
        PostgresTestUtils.shutdown( ds );
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
        Connection conn = OliveUtils.getConnection( ds );
        JDBCContext ctx = new JDBCContext( conn );

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
}
