package za.sabob.olive.transaction;

import java.sql.*;

public interface TransactionCallback {
    
    public void execute( Connection conn ) throws Exception;

}
