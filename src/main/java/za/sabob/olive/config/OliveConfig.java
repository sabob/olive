package za.sabob.olive.config;

import za.sabob.olive.Mode;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OliveConfig {

    private static final Logger LOGGER = Logger.getLogger( OliveConfig.class.getName() );

    /**
     * Specifies the mode in which Olive is running. The default mode is {@link Mode#PRODUCTION}.
     */
    private static Mode mode = Mode.PRODUCTION;

    private static DataSource defaultDataSource;

    private static boolean allowNestingOperations = true;

    public static boolean isAllowNestingOperations() {
        return allowNestingOperations;
    }

    public static void setAllowNestingOperations( boolean allowNestingOperations ) {
        OliveConfig.allowNestingOperations = allowNestingOperations;
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

        OliveConfig.defaultDataSource = defaultDataSource;
    }

    public static DataSource getDefaultDataSource() {

        if ( OliveConfig.defaultDataSource == null ) {
            throw new IllegalStateException(
                    "No default DataSource have been set. Set a default "
                            + "DataSource with JDBCConfig.setDefaultDataSource() or provide a DataSource to the JDBC methods." );
        }

        return defaultDataSource;
    }

    /**
     * Returns the {@link za.sabob.olive.Mode} Olive is running in.
     *
     * @return the mode Olive is running in
     */
    public static Mode getMode() {
        return mode;
    }

    /**
     * Set the {@link za.sabob.olive.Mode} for Olive to run in.
     *
     * @param mode the mode Olive must Run in
     */
    public static void setMode( Mode mode ) {
        OliveConfig.mode = mode;
    }
}
