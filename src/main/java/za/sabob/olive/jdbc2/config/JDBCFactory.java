package za.sabob.olive.jdbc2.config;

import za.sabob.olive.jdbc2.context.*;

public class JDBCFactory {

    private static JDBCFactory FACTORY = new JDBCFactory();

    public static JDBCFactory getInstance() {
        return FACTORY;
    }

    public JDBCContextManager createManager() {
        return new JDBCContextManager();
    }
}
