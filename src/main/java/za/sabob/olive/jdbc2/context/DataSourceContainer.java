package za.sabob.olive.jdbc2.context;

import java.util.*;
import javax.sql.*;
import za.sabob.olive.jdbc2.config.*;

public class DataSourceContainer {

    final private Map<DataSource, JDBCContextManager> managerByDS = new HashMap<>();

    final private Map<JDBCContext, DataSource> dsByContext = new HashMap<>();

    public JDBCContext createTXContext( DataSource ds ) {
        return createContext( ds, true );
    }

    public JDBCContext createContext( DataSource ds ) {
        return createContext( ds, false );
    }

    public JDBCContext getMostRecentJDBCContext( DataSource ds ) {

        JDBCContextManager manager = getManager( ds );
        if ( manager == null ) {
            return null;
        }

        return manager.getMostRecentContext();
    }

    protected JDBCContext createContext( DataSource ds, boolean transactional ) {
        JDBCContextManager manager = getOrCreateManager( ds );

        JDBCContext ctx = manager.createContext( ds, transactional );
        dsByContext.put( ctx, ds );
        return ctx;
    }

    public boolean isEmpty() {
        return managerByDS.isEmpty();
    }

    public boolean isEmpty( DataSource ds ) {
        JDBCContextManager manager = getManager( ds );
        if ( manager == null ) {
            return true;
        }

        return manager.isEmpty();
    }

    protected JDBCContextManager getOrCreateManager( DataSource ds ) {

        JDBCContextManager manager = getManager( ds );

        if ( manager == null ) {
            manager = createManager( ds );
            managerByDS.put( ds, manager );
        }

        return manager;
    }

    public JDBCContextManager getManager( DataSource ds ) {
        return managerByDS.get( ds );
    }

    public JDBCContextManager createManager( DataSource ds ) {
        JDBCContextManager newManager = JDBCFactory.getInstance().createManager();
        return newManager;
    }

    public JDBCContextManager removeManager( JDBCContext ctx, boolean isRootContext ) {
        DataSource ds = dsByContext.remove( ctx );

        if ( isRootContext ) {
            JDBCContextManager manager = managerByDS.remove( ds );
            return manager;
        }
        return null;
    }

}
