package za.sabob.olive.transaction.sync;

import java.sql.*;
import java.util.*;
import za.sabob.olive.util.*;

public class StatementContainer implements AutoCloseable {

    final private Statement st;

    final private List<ResultSet> resultSets = new ArrayList();

    public StatementContainer( Statement st ) {
        this.st = st;
    }

    public void add( ResultSet rs ) {
        if ( resultSets.contains( rs ) ) {
            return;
        }
        resultSets.add( rs );
    }

    public void remove( ResultSet rs ) {
        resultSets.remove( rs );
    }

    public Statement getStatement() {
        return st;
    }

    public List<ResultSet> getResultSets() {
        return resultSets;
    }

    @Override
    public boolean equals( Object obj ) {
        return st.equals( obj );
    }

    @Override
    public int hashCode() {
        return st.hashCode();
    }

    @Override
    public void close() {
        List closeables = getResultSets();
        closeables.add( st );

        OliveUtils.close( closeables );
    }
}
