package za.sabob.olive.jdbc2;

import java.util.*;
import javax.sql.*;
import za.sabob.olive.jdbc2.stack.*;

public class DataSourceContainer {

    final private Map<DataSource, JDBCContextManager> managerByDS = new LinkedHashMap<>();

    public JDBCContext createTXContext( DataSource ds ) {
        return createContext( ds, true );
    }

    public JDBCContext createContext( DataSource ds ) {
        return createContext( ds, false );
    }

    public JDBCContext getMostRecentContext( DataSource ds ) {
        JDBCContextManager manager = getOrCreateManager( ds );
        return manager.getMostRecentContext();
    }

    protected JDBCContext createContext( DataSource ds, boolean transactional ) {
        JDBCContextManager manager = getOrCreateManager( ds );

        JDBCContext ctx = manager.createContext( ds, transactional );
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
            manager = JDBCFactory.getInstance().createManager();
            managerByDS.put( ds, manager );
        }

        return manager;
    }

    protected JDBCContextManager getManager( DataSource ds ) {
        return managerByDS.get( ds );
    }

}
