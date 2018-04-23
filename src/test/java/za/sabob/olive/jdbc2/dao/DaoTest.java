package za.sabob.olive.jdbc2.dao;

import java.sql.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.util.*;

public class DaoTest {

    /**
     * This class shows how a Service will Call a DAO using Olive JDBC. The JDBCContext is passed from the Service to the DAO. The Services
     * uses the doInTransaction.
     */
    public void someLogic( Object entity ) {
        saveInService( entity );
    }

    public void saveInService( Object entity ) {
        JDBC.doInTransaction( ctx -> {
            // TODO how to ensure transaction is passed to other methods now that we have a JDBCContext
            saveInDao( entity, ctx );
        } );

    }

    public void saveInDao( Object o, JDBCContext ctx ) throws SQLException {

        // add ctx.prepareStatement??
        PreparedStatement ps = OliveUtils.prepareStatement( ctx.getConnection(), "update blah" );

        ctx.add( ps );

    }
}
