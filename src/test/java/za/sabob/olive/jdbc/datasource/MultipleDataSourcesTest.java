package za.sabob.olive.jdbc.datasource;

import java.sql.*;
import java.util.*;
import javax.sql.*;

import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.hsqldb.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.config.JDBCConfig;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class MultipleDataSourcesTest {

    DataSource postgresDS;

    DataSource hsqldbDS;

    @BeforeClass( alwaysRun = true )
    public void beforeClass() {
        postgresDS = PostgresTestUtils.createDS();
        hsqldbDS = HSQLDBTestUtils.createDS();
        PostgresTestUtils.createPersonTable( postgresDS );
        HSQLDBTestUtils.createPersonTable( hsqldbDS );
    }

    @AfterClass( alwaysRun = true )
    public void afterClass() throws Exception {
        //ds = new JdbcDataSource();
        //ds = JdbcConnectionPool.create( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", "sa", "sa" );
//        DBTestUtils.shutdown( H2_DS );
//        DBTestUtils.shutdown( HSQL_DS );
        PostgresTestUtils.shutdown( postgresDS );
        HSQLDBTestUtils.shutdown( hsqldbDS );
    }

    //@Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    @Test
    public void basicTest() {

        //insertPersons( hsqlDS);
        insertPersons( postgresDS ); // populate persons in Postgres, NOT HSQL

        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            testPersonsInBothDatabases();

        } catch ( Exception e ) {
            throw new RuntimeException( e );

        } finally {

        }
    }

    public void insertPersons( DataSource ds ) {

        PreparedStatement ps = null;
        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginTransaction( ds );

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );
            ps = OliveUtils.prepareStatement( ctx, "insert into person (name) values(:name)", params );

            ctx.add( ps );

            int count = ps.executeUpdate();

            params.set( "name", "john" );
            count = ps.executeUpdate();

        } catch ( Exception e ) {
            throw JDBC.rollbackTransaction( ctx, e );
//            if ( DBTestUtils.isTimeout( e ) ) {
//                return;
//            }

        } finally {
            JDBC.cleanupTransaction( ctx );
        }
    }

    public void testPersonsInBothDatabases() {

        JDBCContext ctx1 = null;
        JDBCContext ctx2 = null;

        try {

            ctx1 = JDBC.beginTransaction( postgresDS );
            List persons = getLatestPersons( postgresDS, ctx1 );
            Assert.assertEquals( persons.size(), 2 );

            try {
                ctx2 = JDBC.beginTransaction( hsqldbDS );
                persons = getLatestPersons( hsqldbDS, ctx2 );
                Assert.assertEquals( persons.size(), 0 );

            } finally {
                JDBC.cleanupTransaction( ctx2 );
            }

        } finally {
            JDBC.cleanupTransaction( ctx1 );
        }
    }

    public List<Person> getLatestPersons( DataSource ds, JDBCContext ctx ) {

        PreparedStatement ps = OliveUtils.prepareStatement( ctx, "select * from person" );

        List<Person> persons = OliveUtils.mapToBeans( ps, ( ResultSet rs, int rowNum ) -> {
            Person person = new Person();
            person.id = rs.getLong( "id" );
            person.name = rs.getString( "name" );
            return person;
        } );

        return persons;

    }

    public class Person {

        public long id;

        public String name;

    }

}
