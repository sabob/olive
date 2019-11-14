package za.sabob.olive.jdbc;

import za.sabob.olive.jdbc.config.JDBCConfig;
import za.sabob.olive.jdbc.context.JDBCContext;
import za.sabob.olive.jdbc.operation.Operation;
import za.sabob.olive.jdbc.operation.Query;
import za.sabob.olive.jdbc.transaction.TransactionalOperation;
import za.sabob.olive.jdbc.transaction.TransactionalQuery;
import za.sabob.olive.util.OliveUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class JDBC {


    public static JDBCContext createJDBCContext( boolean beginTransaction ) {
        DataSource ds = JDBCConfig.getDefaultDataSource();
        JDBCContext ctx = createJDBCContext( ds, beginTransaction );
        return ctx;
    }

    public static JDBCContext createJDBCContext( DataSource ds, boolean beginTransaction ) {

        JDBCContext ctx = null;

        boolean autoCommit = !beginTransaction;

        Connection conn = getNewConnection( ds, autoCommit );

        ctx = new JDBCContext( conn, beginTransaction );
        return ctx;
    }

    private static Connection getNewConnection( DataSource ds, boolean autoCommit ) {

        Connection conn = null;
        boolean currentAutoCommit = true;

        try {
            conn = OliveUtils.getConnection( ds );
            currentAutoCommit = conn.getAutoCommit();
            OliveUtils.setAutoCommit( conn, autoCommit );
            return conn;

        } catch ( Exception e ) {
            // Restore autoCommit to previous value
            RuntimeException re = OliveUtils.closeQuietly( currentAutoCommit, e, conn );
            throw re;
        }
    }

    public static JDBCContext beginOperation( DataSource ds ) {

        JDBCContext ctx = createJDBCContext( ds, false );
        return ctx;
    }

    public static JDBCContext beginTransaction( DataSource ds ) {

        JDBCContext ctx = createJDBCContext( ds, true );
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

//    public static void cleanupOperation( DataSource ds ) {
//
//        if ( ds == null ) {
//            throw new IllegalArgumentException( "DataSource cannot be null" );
//        }
//
//        DataSourceContainer container = DSF.getDataSourceContainer();
//        JDBCContext ctx = container.getMostRecentJDBCContext( ds );
//        cleanupOperation( ctx );
//    }

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
                SQLException sqle = ( SQLException ) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = rollbackTransactionQuietly( ctx, ex );

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <X extends Exception> void inTransaction( TransactionalOperation<X> operation ) {

        DataSource ds = JDBCConfig.getDefaultDataSource();
        inTransaction( ds, operation );
    }

    public static <R, X extends Exception> R inTransaction( TransactionalQuery<R, X> query ) {

        DataSource ds = JDBCConfig.getDefaultDataSource();
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

                SQLException sqle = ( SQLException ) ex;
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
        DataSource ds = JDBCConfig.getDefaultDataSource();
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
                SQLException sqle = ( SQLException ) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = ex;

            throw ( X ) exception;

        } finally {
            RuntimeException re = cleanupOperationQuietly( ctx, exception );
            exception = OliveUtils.addSuppressed( exception, re );

            if ( exception != null ) {
                throw ( X ) exception;
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
                SQLException sqle = ( SQLException ) ex;
                ex = OliveUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = ex;

        } finally {

            exception = cleanupOperationQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }
}
