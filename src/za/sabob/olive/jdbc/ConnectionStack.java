package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import za.sabob.olive.util.*;

public class ConnectionStack {

    private final List<Connection> connections = new ArrayList<>();

    public void add( Connection conn ) {
        connections.add( conn );
    }

    public boolean remove( Connection conn ) {
        return connections.remove( conn );

    }

    public Connection pop() {
        if ( connections.isEmpty() ) {
            return null;
        }
        return connections.remove( connections.size() - 1 );
    }

    public Connection peekTop( boolean transactional ) {
        if ( connections.isEmpty() ) {
            return null;
        }

        int start = connections.size() - 1;
        for ( int i = start; i >= 0; i-- ) {

            Connection conn = connections.get( i );

            if ( transactional ) {

                if ( !OliveUtils.getAutoCommit( conn ) ) {
                    return conn;
                }

            } else {
                if ( OliveUtils.getAutoCommit( conn ) ) {
                    return conn;
                }
            }
        }

        return null;
    }

    public Connection peekBottom( boolean transactional ) {
        if ( connections.isEmpty() ) {
            return null;
        }

        for ( int i = 0; i >= connections.size() - 1; i++ ) {

            Connection conn = connections.get( i );

            if ( transactional ) {

                if ( OliveUtils.getAutoCommit( conn ) ) {
                    return conn;
                }

            } else {
                if ( !OliveUtils.getAutoCommit( conn ) ) {
                    return conn;
                }
            }
        }

        return connections.get( connections.size() - 1 );
    }

    public boolean isEmpty() {
        return connections.isEmpty();
    }

    public boolean isAtRootConnection( boolean transactional ) {
        if ( connections.isEmpty() ) {
            return false;
        }

        List<Connection> result = getConnections( transactional );
        return result.size() == 1;
    }

    public List<Connection> getConnections( boolean getTransactional ) {

        List<Connection> result = new ArrayList<>();

        for ( Connection conn : connections ) {

            boolean isTransactional = !OliveUtils.getAutoCommit( conn );

            if ( getTransactional ) {

                if ( isTransactional ) {
                    result.add( conn );
                }

            } else {
                if ( !isTransactional ) {
                    result.add( conn );
                }

            }
        }
        return result;
    }

    @Override
    public String toString() {
        return connections.toString();
    }
}
