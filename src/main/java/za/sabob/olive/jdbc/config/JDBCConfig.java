package za.sabob.olive.jdbc.config;

import javax.sql.DataSource;

public class JDBCConfig {

    private static DataSource defaultDataSource;


    public static boolean hasDefaultDataSource() {
        return getDefaultDataSource() != null;
    }

    public static DataSource getDefaultDataSource() {
        return JDBCConfig.defaultDataSource;
    }

    public static void setDefaultDataSource( DataSource defaultDataSource ) {
        JDBCConfig.defaultDataSource = defaultDataSource;
    }


}
