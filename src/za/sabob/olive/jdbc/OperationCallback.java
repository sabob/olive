package za.sabob.olive.jdbc;

import java.sql.*;

public interface OperationCallback {
    
    public void execute( Connection conn ) throws Exception;

}
