package za.sabob.olive.jdbc;

import java.sql.*;
import java.util.*;
import za.sabob.olive.util.*;

public class JDBCContext implements AutoCloseable {

    private Connection connection;

    final private boolean rootConnection;

    private final List<Statement> statements = new ArrayList<>();

    private final List<ResultSet> resultSets = new ArrayList<>();

    public JDBCContext( Connection conn, boolean isRoot ) {
        this.connection = conn;
        rootConnection = isRoot;
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList( statements );
    }

    public List<ResultSet> getResultSets() {
        return Collections.unmodifiableList( resultSets );
    }

    public boolean isRootConnection() {
        return rootConnection;
    }

    public Connection getConnection() {
        return connection;
    }

    public void add( Statement statement ) {

        if ( statements.contains( statement ) ) {
            return;
        }

        this.statements.add( statement );
    }

    public void add( ResultSet rs ) {

        if ( resultSets.contains( rs ) ) {
            return;
        }

        this.resultSets.add( rs );
    }

    public void clear() {
        this.resultSets.clear();
        this.statements.clear();
        this.connection = null;
    }

    @Override
    public void close() {
        List<AutoCloseable> closeables = gatherResources();

        RuntimeException exception = null;

        if ( isRootConnection() ) {
            boolean autoCommit = true;
            exception = OliveUtils.closeSilently( autoCommit, closeables );

        } else {
            // Dont close the connection since we aren't referencing the root connection
            closeables.remove( connection );
            boolean autoCommit = true;
            exception = OliveUtils.closeSilently( autoCommit, closeables );
        }

        OliveUtils.throwAsRuntimeIfException( exception );
    }

    public List<AutoCloseable> gatherResources() {

        List<AutoCloseable> resources = new ArrayList();

        List<ResultSet> resultSets = getResultSets();
        resources.addAll( resultSets );

        List<Statement> statements = getStatements();
        resources.addAll( statements );

        resources.add( connection );

        return resources;
    }

}
