package za.sabob.olive.jdbc2;

import za.sabob.olive.jdbc2.stack.*;

@Deprecated
public class JDBCFactory {
    
    private static JDBCFactory FACTORY = new JDBCFactory();
    
    public static JDBCFactory getInstance() {
        return FACTORY;
    }

    public JDBCService getJDBCService() {
        return new JDBCService();
    }
    
    public JDBCContextPipeline createPipeline() {
        return new JDBCContextPipeline();
    }
}
