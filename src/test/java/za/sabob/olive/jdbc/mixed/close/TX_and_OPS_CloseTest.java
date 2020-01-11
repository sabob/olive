// TODO mix jdbc and tx test
package za.sabob.olive.jdbc.mixed.close;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.postgres.*;
import za.sabob.olive.query.*;
import za.sabob.olive.util.*;

public class TX_and_OPS_CloseTest extends PostgresBaseTest {

    int personsCount = 0;

    @AfterClass(alwaysRun = true)
    @Override
    public void afterClass() {
        super.afterClass();
        Assert.assertEquals( personsCount, 2 );
    }

    //@Test
    public void closeRootContextTest() {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginOperation( ds );

            insertPersons( ctx );

            nestedTX( ds );

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {

            Assert.assertTrue( ctx.isOpen() );
            Assert.assertTrue( JDBCUtils.getAutoCommit( ctx.getConnection() ) );

            JDBC.cleanupOperation( ctx );

            Assert.assertTrue( ctx.isClosed() );
        }
    }

    @Test
    public void closeEachChildContextIndividuallyTest() {

        JDBCContext rootOperationCtx = JDBC.beginOperation( ds );
        Assert.assertTrue( JDBCUtils.getAutoCommit( rootOperationCtx.getConnection() ) );

        try {

            insertPersons( rootOperationCtx );

            JDBCContext childTXCtx = JDBC.beginTransaction( ds );
            Assert.assertFalse( JDBCUtils.getAutoCommit( childTXCtx.getConnection() ) );

            List<Person> persons = getPersons( childTXCtx );
            personsCount = persons.size();


            JDBC.cleanupTransaction( childTXCtx );

            Assert.assertTrue( childTXCtx.isClosed() );
            Assert.assertTrue( childTXCtx.isConnectionClosed() );

            JDBC.cleanupTransaction( childTXCtx ); // calling cleanup on TCX should not make a difference too root, because root is non TX
            //JDBC.cleanupTransaction( rootCtx ); // calling cleanup on TCX should not make a difference too root, because root is non TX

            Assert.assertFalse( rootOperationCtx.getConnection().isClosed() );
            Assert.assertTrue( rootOperationCtx.isOpen() );

            Assert.assertTrue( JDBCUtils.getAutoCommit( rootOperationCtx.getConnection() ) );

            JDBC.cleanupOperation( rootOperationCtx ); // now cleanup should occur because we use cleanup operation instead of TX

            Assert.assertTrue( rootOperationCtx.isConnectionClosed() );
            Assert.assertTrue( rootOperationCtx.isClosed() );

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            JDBC.cleanupOperation( rootOperationCtx );

            Assert.assertTrue( rootOperationCtx.isClosed() );
        }
    }
//
//    private void insertPersons( JDBCContext ctx ) throws SQLException {
//        SqlParams params = new SqlParams();
//        params.set( "name", "bob" );
//        PreparedStatement ps = JDBCUtils.prepareStatement( ctx.getConnection(), "insert into person (name) values(:name)", params );
//
//        ctx.add( ps );
//
//        int count = ps.executeUpdate();
//
//        params.set( "name", "john" );
//        count = ps.executeUpdate();
//    }
//
//    public void nestedJDBC( DataSource ds ) {
//
//        JDBCContext ctx = null;
//
//        try {
//
//            ctx = JDBC.beginOperation( ds );
//
//            nestedTX( ds );
//
//            List<Person> persons = getPersons( ctx );
//
//        } catch ( Exception e ) {
//            //e.printStackTrace();
//
//        } finally {
//            Assert.assertFalse( ctx.isRootContext() );
//            JDBC.cleanupOperation( ctx );
//            Assert.assertTrue( ctx.isRootContext() );
//        }
//    }

    public void nestedTX( DataSource ds ) throws SQLException {

        JDBCContext ctx = null;

        try {

            ctx = JDBC.beginTransaction( ds );

            List<Person> persons = getPersons( ctx );

        } finally {
            Assert.assertFalse( ctx.getConnection().getAutoCommit() );

            JDBC.cleanupTransaction( ctx );

            Assert.assertTrue( ctx.isClosed() );
            Assert.assertFalse( ctx.getConnection().isClosed() );
        }
    }

    public List<Person> getPersons( JDBCContext ctx ) {

        PreparedStatement ps = JDBCUtils.prepareStatement( ctx.getConnection(), "select * from person" );

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
}
