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
import java.util.*;
import java.util.logging.*;
import za.sabob.olive.ps.ParsedSql;
import za.sabob.olive.ps.SqlParam;
import za.sabob.olive.ps.SqlParams;

/**
 *
 */
public class Test {

    public static void main(String[] args) {
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement("select * from information_schema.catalogs c where c.CATALOG_NAME = :name and c.CATALOG_NAME in (:names)");

        SqlParams params = new SqlParams();
        List ids = new ArrayList();
        ids.add("a");
        ids.add(new Object[] {"TEST", "TEST", 5});
        ids.add(new Object[] {"ok", 1});
        params.set("names", ids);
        params.set("name", "TEST");
        String sql = NamedParameterUtils.substituteNamedParameters(parsedSql, params);
        SqlParam[] paramArray = NamedParameterUtils.buildValueArray(parsedSql, params);
        for (SqlParam param : paramArray) {
            System.out.println("name: " + param.getName() + ", value:" + param.getValue());
        }
        System.out.println(sql);
        System.out.println("named params " + parsedSql.getTotalParameterCount());

        try {
            Connection conn = DriverManager.getConnection("jdbc:h2:~/test", "sa", "sa");
            
            PreparedStatement ps = OliveUtils.prepareStatement(conn, parsedSql, params);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                System.out.println("Row:" + rs.getString("CATALOG_NAME"));
            }
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
