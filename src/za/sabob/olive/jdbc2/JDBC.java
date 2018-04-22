package za.sabob.olive.jdbc2;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.jdbc2.operation.*;
import za.sabob.olive.jdbc2.transaction.*;

public class JDBC {

    public static JDBCContext getJDBCContext() {
        DataSource ds = DSF.getDefault();
        return getJDBCContext( ds );
    }

    public static JDBCContext getJDBCContext( DataSource ds ) {
        DataSourceContainer container = DSF.getDataSourceContainer();
        JDBCContext ctx = container.getMostRecentJDBCContext( ds );

        if ( ctx == null ) {
            throw new IllegalStateException( "No default EntityManagerFactory have been registered. Set a default factory or use EMF.get(factoryName) " );
        }

        return ctx;
    }

    public static JDBCContext beginOperation( DataSource ds ) {

        DataSourceContainer container = DSF.getDataSourceContainer();
        JDBCContext ctx = container.createContext( ds );
        return ctx;
    }

    public static JDBCContext beginTransaction( DataSource ds ) {

        DataSourceContainer container = DSF.getDataSourceContainer();
        JDBCContext ctx = container.createTXContext( ds );
        return ctx;
    }

    public static void cleanupOperation( JDBCContext ctx ) {
        ctx.close();
    }

    public static void cleanupTransaction( JDBCContext ctx ) {
        ctx.close();
    }

    public static void cleanupOperation( DataSource ds ) {
        DataSourceContainer container = DSF.getDataSourceContainer();
        JDBCContext ctx = container.getMostRecentJDBCContext( ds );
        cleanupOperation( ctx );
    }

    public static void cleanupTransaction( DataSource ds ) {
        DataSourceContainer container = DSF.getDataSourceContainer();
        JDBCContext ctx = container.getMostRecentJDBCContext( ds );
        cleanupTransaction( ctx );
    }

    public static void commitTransaction( JDBCContext ctx ) {
        ctx.commit();
    }

    public static void rollbackTransaction( JDBCContext ctx ) {
        ctx.rollback();
    }

    public static RuntimeException rollbackTransaction( JDBCContext ctx, Exception e ) {
        return ctx.rollback( e );
    }

    public static RuntimeException rollbackTransactionSilently( JDBCContext ctx, Exception e ) {
        return ctx.rollbackSilently( e );
    }

    public static void doInTransaction( DataSource ds, Transaction transaction ) {

        JDBCContext ctx = null;

        try {
            ctx = beginTransaction( ds );

            transaction.doInTransaction( ctx );

            commitTransaction( ctx );

        } catch ( Exception ex ) {
            throw rollbackTransaction( ctx, ex );

        } finally {

            cleanupTransaction( ctx );
        }
    }

    public static void doOperation( Operation op ) {
        DataSource ds = DSF.getDefault();
        doOperation( ds, op );
    }

    public static void doOperation( DataSource ds, Operation op ) {

        JDBCContext ctx = null;

        try {
            ctx = beginOperation( ds );

            op.doOperation( ctx );

        } catch ( SQLException ex ) {
            throw new RuntimeException( ex );

        } finally {

            cleanupOperation( ctx );
        }
    }

    public static void doInTransaction( Transaction transaction ) {
        DataSource ds = DSF.getDefault();
        doInTransaction( ds, transaction );
    }
}
