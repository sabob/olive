package za.sabob.olive.transaction.sync;

import java.sql.*;
import java.util.*;
import za.sabob.olive.util.*;

public class ConnectionContainer implements AutoCloseable {

    final private Connection conn;

    final private Map<Statement, StatementContainer> statements = new LinkedHashMap<>();

    public ConnectionContainer( Connection conn ) {
        this.conn = conn;
    }

    public StatementContainer getOrCreateContainer( Statement st ) {

        StatementContainer container = getStatementContainer( st );
        if ( container == null ) {
            container = new StatementContainer( st );

            statements.put( st, container );
        }
        return container;

    }

    public void remove( Statement st ) {
        statements.remove( st );
    }

    public StatementContainer getStatementContainer( Statement st ) {
        StatementContainer containr = statements.get( st );
        return containr;
    }

    public Connection getConnection() {
        return conn;
    }

    public List<StatementContainer> getStatementContainers() {
        List<StatementContainer> list = new ArrayList( statements.values() );
        return list;
    }

    @Override
    public boolean equals( Object obj ) {
        return conn.equals( obj );
    }

    @Override
    public int hashCode() {
        return conn.hashCode();
    }

    @Override
    public void close() {

        List<AutoCloseable> closeables = collectCloseablesInReverseOrder();

        OliveUtils.close( closeables );
    }

    public List<AutoCloseable> collectCloseablesInReverseOrder() {
        List<AutoCloseable> closeables = new ArrayList<>();

        List<StatementContainer> statementContainers = getStatementContainers();

        // collect in reverse order of latest added
        for ( int i = statementContainers.size() - 1; i >= 0; i-- ) {

            StatementContainer container = statementContainers.get( i );
            closeables.addAll( container.getResultSets() );
            closeables.add( container.getStatement() );
        }

        closeables.add( conn );

        return closeables;
    }

}
