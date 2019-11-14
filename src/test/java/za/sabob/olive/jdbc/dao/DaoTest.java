package za.sabob.olive.jdbc.dao;

import java.io.*;
import java.sql.*;
import javax.sql.*;
import org.testng.*;
import org.testng.annotations.*;
import za.sabob.olive.domain.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc.config.JDBCConfig;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.postgres.*;
import za.sabob.olive.util.*;

public class DaoTest extends PostgresBaseTest {

    @Test
    public void basicTest() {
        Person person = new Person();
        saveInService( person );
    }

    @Test
    public void exceptionTest() {
        try {
            Person person = new Person();
            saveWithException( person );

            Assert.fail( "exceptoin must be thrown" );
        } catch ( Exception ex ) {
            Assert.assertTrue( ex.getMessage().contains( "BAD IO" ) );

        }
    }

    /**
     * This class shows how a Service will Call a DAO using Olive JDBC. The JDBCContext is passed from the Service to the DAO. The Services
     * uses the doInTransaction.
     */
    public void someLogic( Object entity ) {
        saveInService( entity );
    }

    public Person getPerson( long id ) {

        return JDBC.inTransaction( ctx -> {
            saveInDao( id, ctx );
            saveInService( "" );
            return new Person();
        } );
    }

    public void saveInService( Object entity ) {

        DataSource ds = JDBCConfig.getDefaultDataSource();
        JDBC.inOperation( ds, ctx -> {
            saveInDao( new Person(), ctx );
                      return null;
                  } );

    }

    public void saveWithException( Object entity ) {

        DataSource ds = JDBCConfig.getDefaultDataSource();

        JDBC.inOperation( ds, ctx -> {
            if (true) throw new IOException( "BAD IO" );
        } );
    }

    public void saveInDao( Object o, JDBCContext ctx ) throws IOException {
        PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "update blah" );

        ctx.add( ps );

    }
}
