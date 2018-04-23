package za.sabob.olive.jdbc.transaction;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.ps.*;
import za.sabob.olive.transaction.*;
import za.sabob.olive.util.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.util.*;

public class TransactionTest {

    DataSource ds;

    @BeforeClass
    public void beforeClass() {
        ds = DBTestUtils.createDataSource(DBTestUtils.H2);
        //ds.setURL( "jdbc:h2:~/test" );

        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );

    }

    @Test
    public void basicTest() {
        //Connection conn = OliveUtils.getConnection( "jdbc:h2:~/test", "sa", "sa" );
        JDBCContext ctx = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            ctx = TX.beginTransaction( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "CATALOG_NAME" );
                System.out.println( "Row:" + name );
                Assert.assertEquals( name, "TEST" );
            }

            TX.commitTransaction();

        } catch ( Exception ex ) {
            System.out.println( "ERR" );

            throw TX.rollbackTransaction( ex );

        } finally {
            boolean isAtRoot = TX.isAtRootConnection();
            Assert.assertTrue( isAtRoot );

            TX.cleanupTransaction( ps, rs );

            isAtRoot = TX.isAtRootConnection();
            Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the TX" );
        }
    }

    @Test(successPercentage = 100, threadPoolSize = 20, invocationCount = 100, timeOut = 1110000)
    public void threadTest() {
        JDBCContext ctx = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            ctx = TX.beginTransaction( ds );

            nested( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "CATALOG_NAME" );
                Assert.assertEquals( name, "TEST" );
            }

            TX.commitTransaction();

        } catch ( SQLException ex ) {

            throw TX.rollbackTransaction( ex );

        } finally {
            boolean isAtRoot = TX.isAtRootConnection();
            Assert.assertTrue( isAtRoot );

            TX.cleanupTransaction( ps, rs );

            isAtRoot = TX.isAtRootConnection();
            Assert.assertFalse( isAtRoot, "cleanupTransaction should remove all datasources in the TX" );
        }

    }

    public static void nested( DataSource ds ) {

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            JDBCContext ctx = TX.beginTransaction( ds );

            SqlParams params = new SqlParams();
            ps = OliveUtils.prepareStatement( ctx.getConnection(), "select * from information_schema.catalogs c", params );

            rs = ps.executeQuery();
//
            while ( rs.next() ) {
                String name = rs.getString( "CATALOG_NAME" );
                Assert.assertEquals( name, "TEST" );
            }

            TX.commitTransaction( ctx );

        } catch ( SQLException ex ) {

            throw TX.rollbackTransaction( ex );

        } finally {
            Assert.assertFalse( TX.isAtRootConnection() );
            TX.cleanupTransaction( ps, rs );
            Assert.assertTrue( TX.isAtRootConnection() );
        }

    }
}
