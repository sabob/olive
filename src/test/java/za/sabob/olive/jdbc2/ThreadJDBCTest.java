package za.sabob.olive.jdbc2;

import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.util.*;

public class ThreadJDBCTest {

    DataSource ds;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ds = DBTestUtils.createDataSource( DBTestUtils.H2 );
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        DBTestUtils.shutdown( ds );
    }

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    //@Test(successPercentage = 100, threadPoolSize = 2, invocationCount = 3, timeOut = 1110000)
    public void basicThreadTest() {

        JDBCContext ctx = null;

        try {
            //System.out.println( "* start: " + Thread.currentThread().getId() );
            ctx = JDBC.beginOperation( ds );

            nested( ds );
            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx, "select * from person p", params );
            String name = OliveUtils.mapToPrimitive( String.class, ps );

        } catch ( Exception e ) {
            throw new RuntimeException( e );

        } finally {
            boolean isRoot = ctx.isRootContext();
//            if ( !isRoot ) {
//                JDBCContext root = ctx.getRootContext();
//                DataSourceContainer container = JDBCLookup.getDataSourceContainer();
//                JDBCContextManager manager = container.getOrCreateManager( ds );
//                JDBCContext mostRecent = manager.getRootContext();
//                isRoot = ctx.isRootContext();
//
//            }
            Assert.assertTrue( isRoot );
            JDBC.cleanupOperation( ctx );
            //System.out.println( "* end: " + Thread.currentThread().getId() );

            boolean isAtRoot = ctx.isRootContext();
            Assert.assertTrue( isAtRoot, "cleanupTransaction should remove all datasources in the JDBC Operation" );
        }

    }

    public static void nested( DataSource ds ) {

        JDBCContext ctx = null;

        try {
            ctx = JDBC.beginOperation( ds );

            SqlParams params = new SqlParams();
            PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from person p", params );
            ctx.add( ps );

            ResultSet rs = ps.executeQuery();
            ctx.add( rs );
//
            while ( rs.next() ) {
                String name = rs.getString( "name" );
                //Assert.assertEquals( name, "TEST" );
            }

        } catch ( SQLException e ) {
            throw new RuntimeException( e );

        } finally {
            Assert.assertFalse( ctx.isRootContext() );
            JDBC.cleanupOperation( ctx );
            Assert.assertTrue( ctx.isRootContext() );
        }

    }
}
