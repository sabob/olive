package za.sabob.olive.jdbc2;

import java.sql.*;
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

    public static <X extends Exception> void updateInTransaction( DataSource ds, TransactionUpdater<X> updater ) {

        JDBCContext ctx = null;
        Exception exception = null;

        try {
            ctx = beginTransaction( ds );

            updater.update( ctx );

            commitTransaction( ctx );

        } catch ( Exception ex ) {

            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExcpetionToSuppressed( sqle );
            }

            exception = rollbackTransactionSilently( ctx, ex );

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <R, X extends Exception> R executeInTransaction( TransactionExecutor<R, X> executor ) {

        DataSource ds = DSF.getDefault();
        return executeInTransaction( ds, executor );
    }

    public static <R, X extends Exception> R executeInTransaction( DataSource ds, TransactionExecutor<R, X> executor ) {

        JDBCContext ctx = null;
        RuntimeException exception = null;

        R result = null;

        try {
            ctx = beginTransaction( ds );

            result = executor.execute( ctx );

            commitTransaction( ctx );

            return result;

        } catch ( Exception ex ) {

            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExcpetionToSuppressed( sqle );
            }

            exception = rollbackTransactionSilently( ctx, ex );
            throw exception;

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <R, X extends Exception> R execute( OperationExecutor<R, X> executor ) {
        DataSource ds = DSF.getDefault();
        return execute( ds, executor );
    }

    public static <R, X extends Exception> R execute( DataSource ds, OperationExecutor<R, X> executor ) {

        JDBCContext ctx = null;
        RuntimeException exception = null;

        R result = null;

        try {
            ctx = beginOperation( ds );

            result = executor.execute( ctx );

            return result;

        } catch ( Exception ex ) {

            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExcpetionToSuppressed( sqle );
            }

            exception = OliveUtils.toRuntimeException( ex );
            throw exception;

        } finally {
            exception = cleanupOperationQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <R, X extends Exception> R query( OperationExecutor<R, X> executor ) {
        DataSource ds = DSF.getDefault();
        return query( ds, executor );
    }

    public static <R, X extends Exception> R query( DataSource ds, OperationExecutor<R, X> executor ) {
        return execute( ds, executor );
    }

    public static <X extends Exception> void update( OperationUpdater<X> updater ) {
        DataSource ds = DSF.getDefault();
        update( ds, updater );
    }

    public static <X extends Exception> void update( DataSource ds, OperationUpdater<X> updater ) {

        JDBCContext ctx = null;
        Exception exception = null;

        try {
            ctx = beginOperation( ds );

            updater.update( ctx );

        } catch ( Exception ex ) {
            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExcpetionToSuppressed( sqle );
            }

            exception = ex;

        } finally {

            exception = cleanupOperationQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <X extends Exception> void updateInTransaction( TransactionUpdater<X> updater ) {

        DataSource ds = DSF.getDefault();
        updateInTransaction( ds, updater );
    }

    public static <R, X extends Exception> R queryInTransaction( DataSource ds, TransactionExecutor<R, X> executor ) {
        return executeInTransaction( ds, executor );
    }

    public static <R, X extends Exception> R queryInTransaction( TransactionExecutor<R, X> producer ) {

        DataSource ds = DSF.getDefault();
        return queryInTransaction( ds, producer );
    }
}
