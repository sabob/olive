package za.sabob.olive.jdbc.config;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDBCConfig {

    private static final Logger LOGGER = Logger.getLogger( JDBCConfig.class.getName() );

    private static DataSource defaultDataSource;

    private static boolean allowNestingOperations = true;

    public static boolean isAllowNestingOperations() {
        return allowNestingOperations;
    }

    public static void setAllowNestingOperations( boolean allowNestingOperations ) {
        JDBCConfig.allowNestingOperations = allowNestingOperations;
    }

    public static boolean hasDefaultDataSource() {
        return defaultDataSource != null;
    }

    public static void setDefaultDataSource( DataSource defaultDataSource ) {

        if ( hasDefaultDataSource() ) {
            Throwable t = new Throwable(
                    "You are calling JDBCConfig.setDefaultDataSource()  while there is already a defaultDataSource set. Current dataSource overwritten by new dataSource" );
            LOGGER.log( Level.FINE, t.getMessage(), t );
        }

        JDBCConfig.defaultDataSource = defaultDataSource;
    }

    public static DataSource getDefaultDataSource() {

        if ( JDBCConfig.defaultDataSource == null ) {
            throw new IllegalStateException(
                    "No default DataSource have been set. Set a default "
                            + "DataSource with JDBCConfig.setDefaultDataSource() or provide a DataSource to the JDBC methods." );
        }

        return defaultDataSource;
    }
}
