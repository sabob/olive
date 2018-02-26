package za.sabob.olive.query;

import java.sql.*;

public interface RowMapper<T> {

    public T map( ResultSet rs, int rowNum ) throws SQLException;

}
