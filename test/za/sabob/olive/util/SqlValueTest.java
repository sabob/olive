/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.sabob.olive.util;

import java.sql.*;
import java.util.logging.*;
import za.sabob.olive.ps.ParsedSql;
import za.sabob.olive.ps.SqlParams;

/**
 *
 */
public class SqlValueTest {

    public static void main(String[] args) {
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(
            "select * from information_schema.catalogs c where c.CATALOG_NAME = :name");

        SqlParams params = new SqlParams();

        SqlValue sqlValue = new SqlValue() {

            @Override
            public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
                System.out.println("setValue called for index: " + paramIndex);
                ps.setString(paramIndex, "TEST");
            }
        };

        params.set("name", sqlValue);

        try {
            Connection conn = DriverManager.getConnection("jdbc:h2:~/test", "sa", "sa");

            PreparedStatement ps = OliveUtils.prepareStatement(conn, parsedSql, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("Row:" + rs.getString("CATALOG_NAME"));
            }
        } catch (SQLException ex) {
            Logger.getLogger(SqlValueTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
