package za.sabob.olive.jdbc2;

import za.sabob.olive.jdbc2.context.DataSourceContainer;
import za.sabob.olive.jdbc2.context.JDBCContext;
import javax.sql.*;

public class JDBC {

    public static JDBCContext beginOperation( DataSource ds ) {

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();
        JDBCContext ctx = container.createContext( ds );
        return ctx;
    }

    public static JDBCContext beginTransaction( DataSource ds ) {

        DataSourceContainer container = JDBCLookup.getDataSourceContainer();
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
        DataSourceContainer container = JDBCLookup.getDataSourceContainer();
        JDBCContext ctx = container.getMostRecentContext( ds );
        cleanupOperation( ctx );
    }

    public static void cleanupTransaction( DataSource ds ) {
        DataSourceContainer container = JDBCLookup.getDataSourceContainer();
        JDBCContext ctx = container.getMostRecentContext( ds );
        cleanupTransaction( ctx );
    }

    public static void commitTransaction( JDBCContext ctx ) {
        ctx.commit();
    }
    
    public static void rollbackTransaction( JDBCContext ctx ) {
        ctx.rollback();
    }
    
    public static RuntimeException rollbackTransaction( JDBCContext ctx, Exception e ) {
        return ctx.rollback(e);
    }
    
    public static RuntimeException rollbackTransactionSilently( JDBCContext ctx, Exception e ) {
        return ctx.rollbackSilently(e);
    }
}
