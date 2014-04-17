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

import java.io.*;
import za.sabob.olive.ps.SqlParam;
import za.sabob.olive.ps.ParsedSql;
import za.sabob.olive.ps.NamedParameterUtils;
import za.sabob.olive.ps.SqlParams;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OliveUtils {
    
     /**
     * Represents the end-of-file (or stream).
     */
    public static final int EOF = -1;
    
        /**
     * The default buffer size ({@value}) to use for
     * {@link #copyLarge(InputStream, OutputStream)}
     * and
     * {@link #copyLarge(Reader, Writer)}
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static final Logger LOGGER = Logger.getLogger(OliveUtils.class.getName());

    public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;

    public static void commit(Connection conn) {
        try {
            if (conn != null) {
                conn.commit();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void rollback(Connection conn, SQLException sqle) {

        try {

            if (conn != null) {
                conn.rollback();
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        throw new RuntimeException(sqle);
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void close(ResultSet rs, Statement st, Connection conn) {
        closeResultSet(rs);
        closeStatement(st);
        closeConnection(conn);
    }

    public static void close(ResultSet rs) {
        closeResultSet(rs);
    }

    public static void close(Statement st) {
        closeStatement(st);
    }

    public static void close(Connection conn) {
        closeConnection(conn);
    }

    public static void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeStatement(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static PreparedStatement prepareStatement(Connection conn, ParsedSql parsedSql, SqlParams params) {

        String sql = NamedParameterUtils.substituteNamedParameters(parsedSql, params);
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            setParams(ps, parsedSql, params);
            return ps;

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PreparedStatement prepareStatement(Connection conn, String sqlStr, SqlParams params) {

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sqlStr);
        String sql = NamedParameterUtils.substituteNamedParameters(parsedSql, params);

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            setParams(ps, parsedSql, params);
            return ps;

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void setParams(PreparedStatement ps, ParsedSql sql, SqlParams params) {

        SqlParam[] input = NamedParameterUtils.buildValueArray(sql, params);
        if (input != null) {
            for (int i = 0; i < input.length; i++) {
                SqlParam arg = input[i];
                setParam(ps, i + 1, arg);
            }
        }
    }

    public static void setParam(PreparedStatement ps, int indexPosition, SqlParam param) {

        try {
            StatementUtils.setParameterValue(ps, indexPosition, param);

            /*
             if (param.hasSqlType()) {

             //if (param.getSqlType() == Types.VARCHAR) {
             //  ps.setString(indexPosition, (String) param.getValue());
             //} else {
             if (param.getScale() == null) {
             ps.setObject(indexPosition, param.getValue(), param.getSqlType());

             } else {
             ps.setObject(indexPosition, param.getValue(), param.getSqlType(), param.getScale());
             }

             //}
             } else {
             ps.setObject(indexPosition, param.getValue());
             }
             */
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String truncate(String str, int maxWidth) {
        if (isBlank(str)) {
            return str;
        }

        if (str.length() <= maxWidth) {
            return str;
        }

        return str.substring(0, maxWidth);
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static String toString(final InputStream input) throws IOException {
        final StringBuilderWriter sw = new StringBuilderWriter();
        copy(input, sw, "utf-8");
        return sw.toString();
    }

    public static void copy(final InputStream input, final Writer output, String inputEncoding) throws IOException {
        final InputStreamReader in = new InputStreamReader(input, inputEncoding);
        copy(in, output);
    }

    public static int copy(final Reader input, final Writer output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }
    
     public static long copyLarge(final Reader input, final Writer output) throws IOException {
        return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
    }
     
      public static long copyLarge(final Reader input, final Writer output, final char [] buffer) throws IOException {
        long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println(OliveUtils.truncate("moooo", 2));
    }
}
