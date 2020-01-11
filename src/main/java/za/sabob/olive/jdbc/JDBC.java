package za.sabob.olive.jdbc;

import za.sabob.olive.config.OliveConfig;
import za.sabob.olive.jdbc.operation.Operation;
import za.sabob.olive.jdbc.operation.Query;
import za.sabob.olive.jdbc.transaction.TransactionalOperation;
import za.sabob.olive.jdbc.transaction.TransactionalQuery;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.util.OliveUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDBC {

    private static final Logger LOGGER = Logger.getLogger( JDBC.class.getName() );

    static final ThreadLocal<Map<DataSource, Integer>> OPERATION_STARTED_COUNT = ThreadLocal.withInitial( HashMap::new );

    public static JDBCContext createJDBCContext( boolean beginTransaction ) {
        DataSource ds = OliveConfig.getDefaultDataSource();
        JDBCContext ctx = createJDBCContext( ds, beginTransaction );
        return ctx;
    }

    public static JDBCContext createJDBCContext( DataSource ds, boolean beginTransaction ) {

        JDBCContext ctx = new JDBCContext( ds, beginTransaction );
        return ctx;
    }

    public static JDBCContext beginOperation( DataSource ds ) {

        //operationStarted( ds );

        JDBCContext ctx = createJDBCContext( ds, false );
        return ctx;
    }

    public static JDBCContext beginTransaction( DataSource ds ) {

        boolean beginTransaction = true;
        JDBCContext ctx = createJDBCContext( ds, beginTransaction );
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

        if ( ctx == null ) {
            return;
        }

        ctx.commit();
    }

    public static void rollbackTransaction( JDBCContext ctx ) {

        if ( ctx == null ) {
            return;
        }

        ctx.rollback();
    }

    public static void rollbackTransactionAndThrow( JDBCContext ctx, Exception ex ) {

        if ( ctx == null ) {
            OliveUtils.throwAsRuntimeIfException( ex );
        }
        ctx.rollbackAndThrow( ex );
    }

    public static RuntimeException rollbackTransactionQuietly( JDBCContext ctx, Exception ex ) {

        if ( ctx == null ) {
            return OliveUtils.toRuntimeException( ex );
        }

        return ctx.rollbackQuietly( ex );
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
                ex = JDBCUtils.convertSqlExceptionToSuppressed( sqle );
            }

            if ( ctx == null ) {
                exception = ex;
            } else {
                exception = rollbackTransactionQuietly( ctx, ex );

            }

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <X extends Exception> void inTransaction( TransactionalOperation<X> operation ) {

        DataSource ds = OliveConfig.getDefaultDataSource();
        inTransaction( ds, operation );
    }

    public static <R, X extends Exception> R inTransaction( TransactionalQuery<R, X> query ) {

        DataSource ds = OliveConfig.getDefaultDataSource();
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
                ex = JDBCUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = rollbackTransactionQuietly( ctx, ex );
            throw exception;

        } finally {
            exception = cleanupTransactionQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

    public static <R, X extends Exception> R inOperation( Query<R, X> query ) {
        DataSource ds = OliveConfig.getDefaultDataSource();
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
                ex = JDBCUtils.convertSqlExceptionToSuppressed( sqle );
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
                ex = JDBCUtils.convertSqlExceptionToSuppressed( sqle );
            }

            exception = ex;

        } finally {

            exception = cleanupOperationQuietly( ctx, exception );
            OliveUtils.throwAsRuntimeIfException( exception );
        }
    }

//    public static int getOperationStartedCount( DataSource ds ) {
//        Map<DataSource, Integer> countMap = OPERATION_STARTED_COUNT.get();
//        Integer count = countMap.get( ds );
//    if ( count == null ) {
//        count = 0;
//    }
//        return count;
//    }

    static void operationFinished( DataSource ds ) {

        if ( ds == null ) {
            return;
        }

        Map<DataSource, Integer> countMap = OPERATION_STARTED_COUNT.get();
        Integer count = countMap.get( ds );
        if ( count == null ) {
            count = 0;
        }

        if ( count <= 0 ) {
            count = 0;

            //if ( Olive.getMode() == Mode.DEVELOPMENT ) {
            Throwable t = new Throwable( "Operation already finished. This likely means you called JDBCContext.close() or JDBC.cleanupOperation() twice or without starting an operation" );
            LOGGER.log( Level.WARNING, t.getMessage(), t );
            //}

        } else {
            count--;

            if ( count == 0 ) {
                LOGGER.info( "Operation finished. Currently active operations: 0" );
            }
        }

        countMap.put( ds, count );

    }

    static void operationStarted( DataSource ds ) {

        if ( ds == null ) {
            return;
        }

        Map<DataSource, Integer> countMap = OPERATION_STARTED_COUNT.get();
        Integer count = countMap.get( ds );
        if ( count == null ) {
            count = 0;
        }

        if ( count >= 1 && !OliveConfig.isAllowNestingOperations() ) {
            throw new IllegalStateException( "Nested operations for DataSource " + ds + " are not allowed. Cannot begin a new operation. Close the current active operation or transaction before starting a new operation." );
        }

        count++;
        countMap.put( ds, count );

        LOGGER.info( "Operation started. Currently active operations: " + count );
    }

    static void transactionFinished( DataSource ds ) {

        if ( ds == null ) {
            return;
        }

        Map<DataSource, Integer> countMap = OPERATION_STARTED_COUNT.get();
        Integer count = countMap.get( ds );
        if ( count == null ) {
            count = 0;
        }

        if ( count <= 0 ) {
            count = 0;

            //if ( Olive.getMode() == Mode.DEVELOPMENT ) {
            Throwable t = new Throwable( "Transaction already finished. This likely means you called JDBCContext.close() or JDBC.cleanupTransaction() twice or without starting an transaction" );
            LOGGER.log( Level.WARNING, t.getMessage(), t );
            //}

        } else {
            count--;

            if ( count == 0 ) {
                LOGGER.info( "Transaction finished. Currently active transactions: 0" );
            }
        }
        countMap.put( ds, count );
    }

    static void transactionStarted( DataSource ds ) {

        if ( ds == null ) {
            return;
        }

        Map<DataSource, Integer> countMap = OPERATION_STARTED_COUNT.get();

        Integer count = countMap.get( ds );
        if ( count == null ) {
            count = 0;
        }

        if ( count >= 1 && !OliveConfig.isAllowNestingOperations() ) {
            throw new IllegalStateException( "Nested transaction for DataSource " + ds + " are not allowed. Cannot begin a new transaction. Close the current active transaction or operation before starting a new transaction." );
        }

        count++;
        countMap.put( ds, count );

        LOGGER.info( "Operation started. Currently active transactions: " + count );
    }
}
