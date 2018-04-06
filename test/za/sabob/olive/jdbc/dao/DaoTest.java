package za.sabob.olive.jdbc.dao;

import java.sql.*;
import za.sabob.olive.jdbc.*;
import za.sabob.olive.transaction.*;
import za.sabob.olive.util.*;

public class DaoTest {

    public void someLogic( Object o ) {

/*        
        Requirements            
        ------------
        
        Cannot mix transactional and non-transactional connections
        Can only create one connection and other calls reuse that connection. Cannot start another connection!
        
*/
                


        // TODO how to ensure transaction is passed to other methods now that we have a JDBCContext
        //JDBCContext ctx = null;
        try ( JDBCContext ctx = TX.beginTransaction() ) {

        // TODO how to ensure transaction is passed to other methods now that we have a JDBCContext
            save( o );

            //TX.commitTransaction( ctx );
        } catch ( Exception ex ) {
            // rollback
            //throw TX.rollbackTransaction( ex, ctx );
        }
    }

    public void save( Object o ) {

        JDBCContext ctx = TX.beginTransaction();

        try {
            persist( o, ctx );

            //TX.commitTransaction( ctx );
        } catch ( SQLException ex ) {
            // rollback
            //throw TX.rollbackTransaction( ex, ctx );

        } finally {
            //TX.cleanupTransaction( ctx );
        }

    }

    public void persist( Object o, JDBCContext ctx ) throws SQLException {

        // add ctx.prepareStatement??
        PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "update blah" );

        ctx.add( ps );

    }
}
