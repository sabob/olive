package za.sabob.olive.jdbc.config;

import javax.sql.*;
import za.sabob.olive.jdbc.*;

public class JDBCConfig {

    private static final ThreadLocal<Boolean> JOINABLE_TRANSACTIONS = new ThreadLocal<>();

    private static boolean JOINABLE_TRANSACTIONS_DEFAULT = true;

    public static void setJoinableTransactionsDefault( boolean value ) {
        JOINABLE_TRANSACTIONS_DEFAULT = value;
    }

    public static boolean isJoinableTransactionsDefault() {
        return JOINABLE_TRANSACTIONS_DEFAULT;
    }

    public static boolean setJoinableTransactions( boolean value ) {
        boolean currentValue = isJoinableTransactions();
        JOINABLE_TRANSACTIONS.set( value );
        return currentValue;
    }

    public static boolean isJoinableTransactions() {
        Boolean value = JOINABLE_TRANSACTIONS.get();
        if ( value == null ) {
            return isJoinableTransactionsDefault();
        }
        return value;
    }

    public static DataSource getDefault() {
        return DSF.getDefault();
    }

    public static void registerDefault( DataSource defaultDataSource ) {
        DSF.registerDefault( defaultDataSource );
    }

}
