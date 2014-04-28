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
package za.sabob.olive.ps;

import za.sabob.olive.util.OliveUtils;
import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Provides a class for setting all the named parameters for a SQL statement.
 * <p/>
 * SqlParams consist of a map of {@link SqlParam} instances keyed by named parameter.
 * <p/>
 <code>/sql/person/select-person.sql:</code>
 * <pre class="prettyprint">
 * SELECT * FROM person p WHERE p.name = :name and p.age = :age </pre>
 * <p/>
 * <code>Example.java:</code>
 *
 * <pre class="prettyprint">
 * Connection conn = DriverManager.getConnection("jdbc:...", "username", "password");
 * Olive olive = new Olive();
 * String filename = "/sql/person/select-person.sql";
 * SqlParams params = new SqlParams();
 * * params.setString("name", "Bob");
 * params.setInt("age", 21);
 * PreparedStatement ps = olive.prepareStatement(conn, filename, params); </pre>
 */
public class SqlParams {

    private Map<String, SqlParam> map = new HashMap<String, SqlParam>();

    public SqlParams() {

    }

    public SqlParams(Map<String, ?> paramMap) {
        set(paramMap);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public boolean containsValue(SqlParam value) {
        return map.containsValue(value);
    }

    public SqlParam get(String name) {
        return map.get(name);
    }

    public SqlParams set(SqlParam param) {
        if (param.getName() == null) {
            throw new IllegalStateException("SqlParam name is required!");
        }
        map.put(param.getName(), param);
        return this;
    }

    public SqlParams set(String name, SqlParam param, Object defaultIfNull) {
        if (param.getValue() == null) {
            param.setValue(defaultIfNull);
        }
        map.put(name, param);
        return this;
    }

    public SqlParams set(String name, Object value) {
        return set(name, value, null);
    }

    public SqlParams set(String name, Object value, Object defaultIfNull) {
        if (value instanceof SqlParam) {
            SqlParam sqlParam = (SqlParam) value;
            if (sqlParam.getValue() == null) {
                sqlParam.setValue(defaultIfNull);
            }
            sqlParam.setName(name);
            set(sqlParam);

        } else {
            if (value == null) {
                value = defaultIfNull;
            }
            SqlParam param = new SqlParam(name, value);
            set(param);
        }
        return this;

    }

    public SqlParams set(String name, Object value, int sqlType) {
        return set(name, value, null, (Integer) null);
    }

    public SqlParams set(String name, Object value, Object defaultIfNull, int sqlType) {
        return set(name, value, defaultIfNull, sqlType, (Integer) null);
    }

    public SqlParams set(String name, Object value, int sqlType, Integer scaleOrLength) {
        return set(name, value, null, sqlType, scaleOrLength);
    }

    public SqlParams set(String name, Object value, Object defaultIfNull, int sqlType, Integer scaleOrLength) {
        if (value instanceof SqlParam) {
            SqlParam param = (SqlParam) value;
            param.setSqlType(sqlType);
            param.setScale(scaleOrLength);
            param.setName(name);
            set(param);

        } else {
            if (value == null) {
                value = defaultIfNull;
            }
            SqlParam param = new SqlParam(name, value, sqlType, scaleOrLength);
            set(param);
        }
        return this;
    }

    public SqlParams set(String name, Object value, int sqlType, String typeName) {
        if (value instanceof SqlParam) {
            SqlParam param = (SqlParam) value;
            param.setName(name);
            param.setSqlType(sqlType);
            param.setTypeName(typeName);
            set(param);

        } else {
            SqlParam param = new SqlParam(name, value, sqlType, typeName);
            set(param);
        }
        return this;
    }

    public SqlParams set(String name, Collection<?> collection) {
        SqlParam param = new SqlParam(name, collection);
        set(param);

        return this;
    }

    public SqlParams set(String name, Collection<?> collection, int sqlType) {
        SqlParam param = new SqlParam(name, collection, sqlType);
        set(param);

        return this;
    }

    public SqlParams set(Map<String, ?> paramMap) {
        SqlParams sqlParams = new SqlParams();
        for (Entry<String, ?> entry : paramMap.entrySet()) {
            sqlParams.setObject(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public SqlParams setObject(String name, Object value) {
        return set(name, value);
    }

    public SqlParams setObject(String name, Object value, Object defaultIfNull) {
        return set(name, value, defaultIfNull);
    }

    public SqlParams setObject(String name, Object value, int sqlType) {
        return set(name, value, sqlType);
    }

    public SqlParams setObject(String name, Object value, Object defaultIfNull, int sqlType) {
        return set(name, value, defaultIfNull, sqlType);
    }

    public SqlParams setObject(String name, Object value, int sqlType, int scaleOrLength) {
        return set(name, value, sqlType, (Integer) scaleOrLength);
    }

    public SqlParams setObject(String name, Object value, Object defaultIfNull, int sqlType, int scaleOrLength) {
        return set(name, value, defaultIfNull, sqlType, (Integer) scaleOrLength);
    }

    public SqlParams setString(String name, String value, String defaultIfNull, int maxLength) {
        if (value == null) {
            value = defaultIfNull;
        }
        value = OliveUtils.truncate(value, maxLength);
        return setString(name, value);
    }

    public SqlParams setString(String name, String value, int maxLength) {
        value = OliveUtils.truncate(value, maxLength);
        return setString(name, value);
    }

    public SqlParams setString(String name, String value, String defaultIfNull) {
        SqlParam param = new SqlParam(name, value, Types.VARCHAR);
        return set(name, param, defaultIfNull);
    }

    public SqlParams setString(String name, String value) {
        SqlParam param = new SqlParam(name, value, Types.VARCHAR);
        return set(param);
    }

    public SqlParams setInt(String name, int value) {
        SqlParam param = new SqlParam(name, value, Types.INTEGER);
        return set(param);
    }

    public SqlParams setLong(String name, long value) {
        SqlParam param = new SqlParam(name, value, Types.BIGINT);
        return set(param);
    }

    public SqlParams setDouble(String name, double value) {
        SqlParam param = new SqlParam(name, value, Types.DOUBLE);
        return set(param);
    }

    public SqlParams setNull(String name, int sqlType) {
        SqlParam param = new SqlParam(name, null, sqlType);
        return set(param);
    }

    public SqlParams setNull(String name, int sqlType, String typeName) {
        SqlParam param = new SqlParam(name, null, sqlType, typeName);
        return set(param);
    }

    public SqlParams setBigDecimal(String name, BigDecimal value) {
        SqlParam param = new SqlParam(name, value, Types.DECIMAL);
        return set(param);
    }

    public SqlParams setFloat(String name, float value) {
        SqlParam param = new SqlParam(name, value, Types.FLOAT);
        return set(param);
    }

    public SqlParams setBoolean(String name, boolean value) {
        SqlParam param = new SqlParam(name, value, Types.BOOLEAN);
        return set(param);
    }

    public SqlParams setChar(String name, char value) {
        String str = String.valueOf(value);
        SqlParam param = new SqlParam(name, str, Types.CHAR);
        return set(param);
    }

    public SqlParams setByte(String name, byte value) {
        SqlParam param = new SqlParam(name, value, Types.TINYINT);
        return set(param);
    }

    public SqlParams setBytes(String name, byte[] value) {
        SqlParam param = new SqlParam(name, value, Types.TINYINT);
        return set(param);
    }

    public SqlParams setShort(String name, short value) {
        SqlParam param = new SqlParam(name, value, Types.SMALLINT);
        return set(param);
    }

    public SqlParams setClob(String name, Clob value) {
        SqlParam param = new SqlParam(name, value, Types.CLOB);
        return set(param);
    }

    public SqlParams setClob(String name, Reader value) {
        SqlParam param = new SqlParam(name, value, Types.CLOB);
        return set(param);
    }

    /*
     we need way to store long before exposing this method
     public SqlParams setClob(String name, Reader value, long length) {
     SqlParam param = new SqlParam(name, value, Types.CLOB);
     return set(name, param);
     }
     */
    public SqlParams setBlob(String name, Blob value) {
        SqlParam param = new SqlParam(name, value, Types.BLOB);
        return set(param);
    }

    public SqlParams setBlob(String name, InputStream value) {
        SqlParam param = new SqlParam(name, value, Types.BLOB);
        return set(param);
    }

    /*
     we need way to store long before exposing this method
     public SqlParams setBlob(String name, InputStream value, long length) {
     SqlParam param = new SqlParam(name, value, Types.BLOB);
     return set(name, param);
     }*/
    public SqlParams setDate(String name, java.sql.Date value) {
        SqlParam param = new SqlParam(name, value, Types.DATE);
        return set(param);
    }

    public SqlParams setDate(String name, java.util.Date value) {
        SqlParam param = new SqlParam(name, value, Types.DATE);
        return set(param);
    }

    public SqlParams setTime(String name, Time value) {
        SqlParam param = new SqlParam(name, value, Types.TIME);
        return set(param);
    }

    public SqlParams setTimestamp(String name, Timestamp value) {
        SqlParam param = new SqlParam(name, value, Types.TIMESTAMP);
        return set(param);
    }

    public SqlParams setAsciiStream(String name, InputStream value) {
        SqlParam param = new SqlParam(name, value, Types.LONGVARCHAR);
        return set(param);
    }

    /* Need a way to store the length param before exposing this method
     public SqlParams setAsciiStream(String name, InputStream value, long length) {
     SqlParam param = new SqlParam(name, value, Types.LONGVARCHAR);
     return set(name, param);
     }
    
     public SqlParams setAsciiStream(String name, InputStream value, int length)  {
     SqlParam param = new SqlParam(name, value, Types.LONGVARCHAR);
     return set(name, param);
     }*/
    public SqlParams setBinaryStream(String name, InputStream value) {
        SqlParam param = new SqlParam(name, value, Types.LONGVARBINARY);
        return set(param);
    }

    /*
     Need a way to store the length param before exposing this method
     public SqlParams setBinaryStream(String name, InputStream value, int length) {
     SqlParam param = new SqlParam(name, value, Types.LONGVARBINARY);
     return set(name, param);
     }
    
     public SqlParams setBinaryStream(String name, InputStream value, long length)  {
     SqlParam param = new SqlParam(name, value, Types.LONGVARBINARY);
     return set(name, param);
     }
     */
    public SqlParams setCharacterStream(String name, Reader value) {
        SqlParam param = new SqlParam(name, value, Types.LONGVARCHAR);
        return set(param);
    }

    /*
     Need a way to store the length param before exposing this method
     public SqlParams setCharacterStream(String name, Reader value, int length)  {
     SqlParam param = new SqlParam(name, value, Types.LONGVARCHAR);
     return set(name, param);
     }
    
     public SqlParams setCharacterStream(String name, Reader value, long length) {
     SqlParam param = new SqlParam(name, value, Types.LONGVARCHAR);
     return set(name, param);
     }*/
    public SqlParams setRef(String name, Ref value) {
        SqlParam param = new SqlParam(name, value, Types.REF);
        return set(param);
    }

    public SqlParams setArray(String name, Array value) {
        SqlParam param = new SqlParam(name, value, Types.ARRAY);
        return set(param);
    }

    public SqlParams setURL(String name, URL value) {
        SqlParam param = new SqlParam(name, value, Types.DATALINK);
        return set(param);
    }

    public SqlParams setRowId(String name, RowId value) {
        SqlParam param = new SqlParam(name, value, Types.ROWID);
        return set(param);
    }

    public SqlParams setNString(String name, String value) {
        SqlParam param = new SqlParam(name, value, Types.NVARCHAR);
        return set(param);
    }

    public SqlParams setNCharacterStream(String name, Reader value) {
        SqlParam param = new SqlParam(name, value, Types.LONGNVARCHAR);
        return set(param);
    }

    /*
     Need a way to store the length param before exposing this method
     public SqlParams setNCharacterStream(String name, Reader value, long length) {
     SqlParam param = new SqlParam(name, value, Types.NVARCHAR);
     return set(name, param);
     }*/
    public SqlParams setNClob(String name, NClob value) {
        SqlParam param = new SqlParam(name, value, Types.NCLOB);
        return set(param);
    }

    public SqlParams setNClob(String name, Reader value) {
        SqlParam param = new SqlParam(name, value, Types.NCLOB);
        return set(param);
    }

    /*
     Need a way to store the length param before exposing this method
     public SqlParams setNClob(String name, Reader value, long length) {
     SqlParam param = new SqlParam(name, value, Types.NCLOB);
     return set(name, param);
     }*/
    public SqlParams setSQLXML(String name, SQLXML value) {
        SqlParam param = new SqlParam(name, value, Types.SQLXML);
        return set(param);
    }

    public Object remove(String key) {
        return map.remove(key);
    }

    public SqlParams clear() {
        map.clear();
        return this;
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<SqlParam> values() {
        return map.values();
    }

    public Set<Entry<String, SqlParam>> entrySet() {
        return map.entrySet();
    }

}
