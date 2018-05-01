package za.sabob.olive.jdbc2;

import javax.sql.*;
import za.sabob.olive.jdbc2.context.*;
import za.sabob.olive.jdbc2.operation.*;
import za.sabob.olive.jdbc2.transaction.*;
import za.sabob.olive.util.*;

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

        if ( ctx == null ) {
            return;
        }

        ctx.close();
    }

    public static RuntimeException cleanupOperationQuietly( JDBCContext ctx ) {

        if ( ctx == null ) {
            return null;
        }

        RuntimeException ex = ctx.closeQuietly();

        return ex;
    }

    public static RuntimeException cleanupOperationQuietly( JDBCContext ctx, Exception exception ) {

        if ( ctx == null ) {
            return OliveUtils.toRuntimeException( exception );
        }

        RuntimeException ex = cleanupOperationQuietly( ctx );
        exception = OliveUtils.addSuppressed( ex, exception );
        return OliveUtils.toRuntimeException( exception );
    }

    public static void cleanupOperation( DataSource ds ) {

        if ( ds == null ) {
            throw new IllegalArgumentException( "DataSource cannot be null" );
        }

        DataSourceContainer container = DSF.getDataSourceContainer();
        JDBCContext ctx = container.getMostRecentJDBCContext( ds );
        cleanupOperation( ctx );
    }

    public static void cleanupTransaction( JDBCContext ctx ) {
        cleanupOperation( ctx );
    }

    public static RuntimeException cleanupTransactionQuietly( JDBCContext ctx ) {
        return cleanupOperationQuietly( ctx );
    }

    public static RuntimeException cleanupTransactionQuietly( JDBCContext ctx, Exception exception ) {
        RuntimeException ex = cleanupTransactionQuietly( ctx );
        exception = OliveUtils.addSuppressed( ex, exception );
        return OliveUtils.toRuntimeException( exception );
    }

    public static void cleanupTransaction( DataSource ds ) {

        if ( ds == null ) {
            throw new IllegalArgumentException( "DataSource cannot be null" );
        }

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
        Exception exception = null;

        try {
            ctx = beginTransaction( ds );

            transaction.doInTransaction( ctx );

            commitTransaction( ctx );

        } catch ( Exception ex ) {
            exception = rollbackTransactionSilently( ctx, ex );

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static void doOperation( Operation op ) {
        DataSource ds = DSF.getDefault();
        doOperation( ds, op );
    }

    public static void doOperation( DataSource ds, Operation op ) {

        JDBCContext ctx = null;
        Exception exception = null;

        try {
            ctx = beginOperation( ds );

            op.doOperation( ctx );

        } catch ( Exception ex ) {
            exception = ex;

        } finally {

            exception = cleanupOperationQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static void doInTransaction( Transaction transaction ) {
        DataSource ds = DSF.getDefault();
        doInTransaction( ds, transaction );
    }
}
