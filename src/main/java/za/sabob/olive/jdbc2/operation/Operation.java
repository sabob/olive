package za.sabob.olive.jdbc2.operation;

import java.sql.*;
import za.sabob.olive.jdbc2.context.*;

public interface Operation {

    public void doOperation( JDBCContext ctx ) throws SQLException;
}
