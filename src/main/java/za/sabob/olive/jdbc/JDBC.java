package za.sabob.olive.jdbc;

import java.sql.*;
import javax.sql.*;
import za.sabob.olive.jdbc.context.*;
import za.sabob.olive.jdbc.operation.*;
import za.sabob.olive.jdbc.transaction.*;
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

    public static RuntimeException rollbackTransactionQuietly( JDBCContext ctx, Exception e ) {
        return ctx.rollbackQuietly( e );
    }

    public static <X extends Exception> void inTransaction( DataSource ds, TransactionalOperation<X> operation ) {

        JDBCContext ctx = null;
        Exception exception = null;

        try {
            ctx = beginTransaction( ds );

            operation.run( ctx );

            commitTransaction( ctx );

        } catch ( Exception ex ) {

            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = rollbackTransactionQuietly( ctx, ex );

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <X extends Exception> void inTransaction( TransactionalOperation<X> operation ) {

        DataSource ds = DSF.getDefault();
        inTransaction( ds, operation );
    }

    public static <R, X extends Exception> R inTransaction( TransactionalQuery<R, X> query ) {

        DataSource ds = DSF.getDefault();
        return inTransaction( ds, query );
    }

    public static <R, X extends Exception> R inTransaction( DataSource ds, TransactionalQuery<R, X> query ) {

        JDBCContext ctx = null;
        RuntimeException exception = null;

        try {
            ctx = beginTransaction( ds );

            R result = query.get( ctx );

            commitTransaction( ctx );

            return result;

        } catch ( Exception ex ) {

            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = rollbackTransactionQuietly( ctx, ex );
            throw exception;

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <R, X extends Exception> R inOperation( Query<R, X> query ) {
        DataSource ds = DSF.getDefault();
        return JDBC.inOperation( ds, query );
    }

    public static <R, X extends Exception> R inOperation( DataSource ds, Query<R, X> query ) {

        try {
            return inOperationWithException( ds, query );

        } catch ( Exception ex ) {
            RuntimeException re = OliveUtils.toRuntimeException( ex );
            throw re;
        }

//        JDBCContext ctx = null;
//        RuntimeException exception = null;
//
//        R result = null;
//
//        try {
//            ctx = beginOperation( ds );
//
//            result = query.get( ctx );
//
//            return result;
//
//        } catch ( Exception ex ) {
//
//            if ( ex instanceof SQLException ) {
//                SQLException sqle = (SQLException) ex;
//                ex = OliveUtils.convertSqlExcpetionToSuppressed( sqle );
//            }
//
//            exception = OliveUtils.toRuntimeException( ex );
//            throw exception;
//
//        } finally {
//            exception = cleanupOperationQuietly( ctx, exception );
//            RuntimeException re = new RuntimeException( "SQL SUCS" );
//            OliveUtils.addSuppressed( exception, re );
//            OliveUtils.throwAsRuntimeIfException( exception );
//        }
    }

    private static <R, X extends Exception> R inOperationWithException( DataSource ds, Query<R, X> query ) throws X {

        JDBCContext ctx = null;
        Exception exception = null;

        R result = null;

        try {
            ctx = beginOperation( ds );

            result = query.get( ctx );

            return result;

        } catch ( Exception ex ) {

            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = ex;

            throw (X) exception;

        } finally {
            RuntimeException re = cleanupOperationQuietly( ctx, exception );
            exception = OliveUtils.addSuppressed( exception, re );

            if ( exception != null ) {
                throw (X) exception;
            }
        }
    }

    public static <X extends Exception> void inOperation( DataSource ds, Operation<X> operation ) {

        JDBCContext ctx = null;
        Exception exception = null;

        try {
            ctx = beginOperation( ds );

            operation.run( ctx );

        } catch ( Exception ex ) {
            if ( ex instanceof SQLException ) {
                SQLException sqle = (SQLException) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = ex;

        } finally {

            exception = cleanupOperationQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }
}
