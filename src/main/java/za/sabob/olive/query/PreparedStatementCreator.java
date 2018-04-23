package za.sabob.olive.query;

import java.sql.*;

public interface PreparedStatementCreator {
    
    public PreparedStatement create(Connection conn);

}
