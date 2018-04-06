package za.sabob.olive.jdbc2;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import za.sabob.olive.util.*;

public class MemLeakTest {
    
    public void run() throws Exception {
        
        DataSource ds = DBTestUtils.createDataSource( DBTestUtils.H2 );
        DBTestUtils.createPersonTable( ds, DBTestUtils.H2 );
        
        // Referencing the list will keep the Holder class in memory, removin the reference will GC the holder
        List list = createConn( ds );
        createConn( ds );
        
        System.out.println( "Sleeping" );        
        
        Thread.sleep(10000000);
    }

    public static void main( String[] args ) throws Exception {

        new MemLeakTest().run();
    }

    public static List createConn( DataSource ds ) throws SQLException {
        Connection conn = ds.getConnection();

        List list = new ArrayList();
        
        Holder holder = new Holder();
        holder.setConn( conn);
        
        
        list.add( holder );

        System.out.println( list );

        System.gc();
        return list;

    }
    
    static class Holder {
        private Connection conn;

        public Connection getConn() {
            return conn;
        }

        public void setConn( Connection conn ) {
            this.conn = conn;
        }
        
        
    }
}
