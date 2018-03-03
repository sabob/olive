package za.sabob.olive.transaction.sync;

import java.sql.*;
import java.util.*;
import za.sabob.olive.util.*;

public class JDBCContainer implements AutoCloseable {

    final private Map<Connection, ConnectionContainer> connections = new LinkedHashMap<>();

    public JDBCContainer() {
    }

    public ConnectionContainer createContainer( Connection conn ) {
        ConnectionContainer container = new ConnectionContainer( conn );
        connections.put( conn, container );
        return container;
    }

    public ConnectionContainer getOrCreateContainer( Connection conn ) {
        ConnectionContainer container = getConnectionContainer( conn );

        if ( container == null ) {
            container = createContainer( conn );
        }

        return container;
    }

    public ConnectionContainer getLatestContainer( ) {
        List <ConnectionContainer> containers = getConnectionContainers();
        
        if (containers.isEmpty()) {
            return null;
        }

        ConnectionContainer container = containers.get( containers.size() - 1 );
        return container;
    }

    public void removeContainer( Connection conn ) {
        connections.remove( conn );
    }

    public ConnectionContainer getConnectionContainer( Connection conn ) {
        ConnectionContainer container = connections.get( conn );
        return container;
    }

    public List<ConnectionContainer> getConnectionContainers() {
        List<ConnectionContainer> list = new ArrayList( connections.values() );
        return list;
    }

    @Override
    public void close() {

        List<AutoCloseable> closeables = collectCloseablesInReverseOrder();

        OliveUtils.close( closeables );
    }

    public List<AutoCloseable> collectCloseablesInReverseOrder() {

        List<AutoCloseable> closeables = new ArrayList<>();

        List<ConnectionContainer> connectionContainers = getConnectionContainers();

        // collect in reverse order of latest added
        for ( int i = connectionContainers.size() - 1; i >= 0; i-- ) {

            ConnectionContainer container = connectionContainers.get( i );

            List children = container.collectCloseablesInReverseOrder();
            closeables.addAll( children );
        }

        return closeables;
    }

}
