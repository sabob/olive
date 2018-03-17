package za.sabob.olive.jdbc.datasource;

import za.sabob.olive.util.DBTestUtils;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.query.*;
import za.sabob.olive.transaction.TX;
import za.sabob.olive.util.*;

public class MultipleDataSourcesTest {

    DataSource H2_DS;

    DataSource HSQL_DS;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        H2_DS = DBTestUtils.createDataSource( DBTestUtils.H2 );
        HSQL_DS = DBTestUtils.createDataSource( DBTestUtils.HSQLDB );
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable(H2_DS, DBTestUtils.H2 );
        DBTestUtils.createPersonTable(HSQL_DS, DBTestUtils.HSQLDB );
    }
    
    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
        DBTestUtils.shutdown( H2_DS );
        DBTestUtils.shutdown( HSQL_DS );
    }

    //@Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    @Test
    public void basicTest() {

        //insertPersons( hsqlDS);
        insertPersons(H2_DS ); // populate persons in H2, NOT HSQL

        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            testPersons();

        } catch ( Exception e ) {
            throw new RuntimeException( e );

        } finally {

        }
    }

    public void insertPersons( DataSource ds ) {

        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = TX.beginTransaction( ds );

            SqlParams params = new SqlParams();
            params.set( "name", "Bob" );
            ps = OliveUtils.prepareStatement( conn, "insert into person (name) values(:name)", params );

            int count = ps.executeUpdate();

            params.set( "name", "John" );
            count = ps.executeUpdate();

        } catch ( Exception e ) {
            if ( DBTestUtils.isTimeout( e ) ) {
                return;
            }

            throw new RuntimeException( e );

        } finally {
            TX.cleanupTransaction( conn, ps );
        }
    }

    public void testPersons() {

        try {

            Connection h2Conn = TX.beginTransaction(H2_DS );
            List persons = getLatestPersons();
            Assert.assertEquals( persons.size(), 2 );

            try {
                Connection hsqlConn = TX.beginTransaction(HSQL_DS );
                persons = getLatestPersons();
                Assert.assertEquals( persons.size(), 0 );
            } finally {
                TX.cleanupTransaction();
            }

        } finally {
            TX.cleanupTransaction();
        }
    }

    public List<Person> getLatestPersons() {

        Connection conn = JDBCContext.getLatestConnection();

        PreparedStatement ps = OliveUtils.prepareStatement( conn, "select * from person" );

        List<Person> persons = OliveUtils.query( ps, new RowMapper<Person>() {
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
