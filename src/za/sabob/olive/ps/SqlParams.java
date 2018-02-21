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
 * This class provides the same <em>setter</em> methods found on {@link java.sql.PreparedStatement} such as
 * {@link #setObject(java.lang.String, java.lang.Object)}, {@link #setString(java.lang.String, java.lang.String)},
 * {@link #setInt(java.lang.String, int)} etc.
 * <p/>
 * SqlParams consist of a map of {@link SqlParam} instances keyed by named parameter.
 * <p/>
 * <code>/sql/person/select-person.sql:</code>
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

    /**
     * The map of named parameters keyed on named parameter which value is a {@link SqlParam}.
     */
    protected Map<String, SqlParam> paramMap = new HashMap<String, SqlParam>();

    /**
     * Creates a new default SqlParams instance.
     */
    public SqlParams() {

    }

    /**
     * Creates a new default SqlParams instance for the given paramMap. The values in the paramMap will be used to populate this SqlParams
     * instance.
     *
     * @param paramMap the map of value to populate the SqlParams with
     */
    public SqlParams(Map<String, ?> paramMap) {
        set(paramMap);
    }

    /**
     * Returns the size of this SqlParams.
     *
     * @return the size of this SqlParams
     */
    public int size() {
        return paramMap.size();
    }

    /**
     * Return true if this SqlParams is empty, false otherwise.
     *
     * @return true if this SqlParams is empty, false otherwise
     */
    public boolean isEmpty() {
        return paramMap.isEmpty();
    }

    /**
     * Returns true if this SqlParams contains the named parameter for the given name.
     *
     * @param name the name whose presence in this SqlParams is to be tested
     * @return true if the name is contained by this SqlParams instance
     */
    public boolean containsKey(String name) {
        return paramMap.containsKey(name);
    }

    /**
     * Returns true if this SqlParams contains the parameter for the given value.
     *
     * @param value the value whose presence in this SqlParams is to be tested
     * @return true if the value is contained by this SqlParams instance
     */
    public boolean containsValue(SqlParam value) {
        return paramMap.containsValue(value);
    }

    /**
     * Return the {@link SqlParam} for the given name, or null if the name is not found.
     *
     * @param name the name of the SqlParam to return
     * @return the SqlParam for the given name
     */
    public SqlParam get(String name) {
        return paramMap.get(name);
    }

    /**
     * Set a named parameter for the given {@link SqlParam} on the {@link #paramMap parameter map}.
     * <p/>
     * The SqlParam will be added to the {@link #paramMap parameter map} under it's {@link SqlParam#name}.
     * <p/>
     * The SqlParam must have {@link SqlParam#getName() name} defined, else a {@link IllegalArgumentException} is thrown.
     *
     * @param sqlParam the SqlParam to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the {@link SqlParam#getName() name} is not defined
     */
    public SqlParams set(SqlParam sqlParam) {
        if (sqlParam.getName() == null) {
            throw new IllegalArgumentException("SqlParam name is required!");
        }
        paramMap.put(sqlParam.getName(), sqlParam);
        return this;
    }

    /**
     * Set a named parameter for the given {@link SqlParam} on the {@link #paramMap parameter map}. If the {@link SqlParam#value} is
     * null, defaultIfNull will be used instead.
     * <p/>
     * The SqlParam will be added to the {@link #paramMap parameter map} under it's {@link SqlParam#name}.
     * <p/>
     * The SqlParam must have {@link SqlParam#getName() name} defined, else a {@link IllegalArgumentException} is thrown.
     *
     * @param sqlParam the SqlParam to set
     * @param defaultIfNull default value to use if {@link SqlParam#value} is null
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the sqlParam name is not defined
     */
    public SqlParams set(SqlParam sqlParam, Object defaultIfNull) {
        if (sqlParam.getValue() == null) {
            sqlParam.setValue(defaultIfNull);
        }
        set(sqlParam);
        return this;
    }

    /**
     * Set a named parameter for the given given name and value on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and value and set on the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value) {
        return set(name, value, null);
    }

    /**
     * Set a named parameter for the given given name and value on the {@link #paramMap parameter map}. If the value is
     * null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and value and set on the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param defaultIfNull default value to use if value is null
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value, Object defaultIfNull) {
        if (value instanceof SqlParam) {
            SqlParam sqlParam = (SqlParam) value;
            sqlParam.setName(name);
            set(sqlParam, defaultIfNull);

        } else {
            if (value == null) {
                value = defaultIfNull;
            }
            SqlParam param = new SqlParam(name, value);
            set(param);
        }
        return this;

    }

    /**
     * Set a named parameter for the given given name, value and {@link java.sql.Types sqlType} on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value and {@link java.sql.Types sqlType} and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value, int sqlType) {
        return set(name, value, null, sqlType);
    }

    /**
     * Set a named parameter for the given given name, value and {@link java.sql.Types sqlType} on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value and {@link java.sql.Types sqlType} and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param defaultIfNull default value to use if value is null
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value, Object defaultIfNull, int sqlType) {
        return set(name, value, defaultIfNull, sqlType, 0);
    }

    /**
     * Set a named parameter for the given given name, value, {@link java.sql.Types sqlType} and scaleOrLength on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value, {@link java.sql.Types sqlType} and scaleOrLength and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param scaleOrLength the number of digits after the decimal
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value, int sqlType, Integer scaleOrLength) {
        return set(name, value, null, sqlType, scaleOrLength);
    }

    /**
     * Set a named parameter for the given given name, value, {@link java.sql.Types sqlType} and scaleOrLength on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value, {@link java.sql.Types sqlType} and scaleOrLength and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param defaultIfNull default value to use if value is null
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param scaleOrLength the number of digits after the decimal
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value, Object defaultIfNull, int sqlType, Integer scaleOrLength) {
        if (value instanceof SqlParam) {
            SqlParam param = (SqlParam) value;
            param.setSqlType(sqlType);
            param.setScale(scaleOrLength);
            param.setName(name);
            set(param, defaultIfNull);

        } else {
            if (value == null) {
                value = defaultIfNull;
            }
            SqlParam param = new SqlParam(name, value, sqlType, scaleOrLength);
            set(param);
        }
        return this;
    }

    /**
     * Set a named parameter for the given given name, value, {@link java.sql.Types sqlType} and typeName on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value, {@link java.sql.Types sqlType} and scaleOrLength and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param typeName the fully-qualified name of an SQL user-defined type; ignored if the parameter is not a user-defined type
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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

    /**
     * Set a named parameter for the given given name, value, {@link java.sql.Types sqlType} and typeName on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value, {@link java.sql.Types sqlType} and scaleOrLength and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @param defaultIfNull default value to use if value is null
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param typeName the fully-qualified name of an SQL user-defined type; ignored if the parameter is not a user-defined type
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Object value, Object defaultIfNull, int sqlType, String typeName) {
        if (value instanceof SqlParam) {
            SqlParam param = (SqlParam) value;

            param.setName(name);
            param.setSqlType(sqlType);
            param.setTypeName(typeName);
            set(param, defaultIfNull);

        } else {
            if (value == null) {
                value = defaultIfNull;
            }
            SqlParam param = new SqlParam(name, value, sqlType, typeName);
            set(param);
        }
        return this;
    }

    /**
     * Set a named parameter for the given given name and Collection on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Collection and set on the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Collection of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Collection<?> value) {
        SqlParam param = new SqlParam(name, value);
        set(param);

        return this;
    }

    /**
     * Set a named parameter for the given given name, Collection and {@link java.sql.Types sqlType} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, Collection and {@link java.sql.Types sqlType} and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Collection of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the Collection
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams set(String name, Collection<?> value, int sqlType) {
        SqlParam param = new SqlParam(name, value, sqlType);
        set(param);

        return this;
    }

    /**
     * Set named parameters for the given given map on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for each item in the map and set on the {@link #paramMap parameter map}.
     *
     * @param paramMap map of named parameters to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if a name on the map is not defined
     */
    public SqlParams set(Map<String, ?> paramMap) {
        for (Entry<String, ?> entry : paramMap.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Set a named parameter for the given given name and Object on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and value and set on the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Object of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setObject(String name, Object value) {
        return set(name, value);
    }

    /**
     * Set a named parameter for the given given name and Object on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and value and set on the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Object of the named parameter to set
     * @param defaultIfNull default value to use if the Object is null
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setObject(String name, Object value, Object defaultIfNull) {
        return set(name, value, defaultIfNull);
    }

    /**
     * Set a named parameter for the given given name, Object and {@link java.sql.Types sqlType} on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value and {@link java.sql.Types sqlType} and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Object of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setObject(String name, Object value, int sqlType) {
        return set(name, value, sqlType);
    }

    /**
     * Set a named parameter for the given given name, Object and {@link java.sql.Types sqlType} on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value and {@link java.sql.Types sqlType} and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Object of the named parameter to set
     * @param defaultIfNull default value to use if the Object is null
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setObject(String name, Object value, Object defaultIfNull, int sqlType) {
        return set(name, value, defaultIfNull, sqlType);
    }

    /**
     * Set a named parameter for the given given name, Object, {@link java.sql.Types sqlType} and scaleOrLength on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value, {@link java.sql.Types sqlType} and scaleOrLength and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Object of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param scaleOrLength the number of digits after the decimal
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setObject(String name, Object value, int sqlType, int scaleOrLength) {
        return set(name, value, sqlType, (Integer) scaleOrLength);
    }

    /**
     * Set a named parameter for the given given name, Object, {@link java.sql.Types sqlType} and scaleOrLength on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, value, {@link java.sql.Types sqlType} and scaleOrLength and set on the
     * {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param value the Object of the named parameter to set
     * @param defaultIfNull default value to use if the Object is null
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param scaleOrLength the number of digits after the decimal
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setObject(String name, Object value, Object defaultIfNull, int sqlType, int scaleOrLength) {
        return set(name, value, defaultIfNull, sqlType, (Integer) scaleOrLength);
    }

    /**
     * Set a named parameter for the given given name and String on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and String and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#VARCHAR}.
     * <p/>
     * If the String's length exceeds the given maxLength, the String will be truncated to the maxLength.
     *
     * @param name the name of the named parameter to set
     * @param value the String of the named parameter to set
     * @param defaultIfNull default value to use if String is null
     * @param maxLength truncate the String if it exceeds the maxLength
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setString(String name, String value, String defaultIfNull, int maxLength) {
        if (value == null) {
            value = defaultIfNull;
        }
        value = OliveUtils.truncate(value, maxLength);
        return setString(name, value);
    }

    /**
     * Set a named parameter for the given given name and String on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and String and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#VARCHAR}.
     * <p/>
     * If the String's length exceeds the given maxLength, the String will be truncated to the maxLength.
     *
     * @param name the name of the named parameter to set
     * @param value the String of the named parameter to set
     * @param maxLength truncate the String if it exceeds the maxLength
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setString(String name, String value, int maxLength) {
        value = OliveUtils.truncate(value, maxLength);
        return setString(name, value);
    }

    /**
     * Set a named parameter for the given given name and String on the
     * {@link #paramMap parameter map}. If the value is null, defaultIfNull will be used instead.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and String and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#VARCHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the String of the named parameter to set
     * @param defaultIfNull default value to use if String is null
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setString(String name, String value, String defaultIfNull) {
        SqlParam param = new SqlParam(name, value, Types.VARCHAR);
        return set(param, defaultIfNull);
    }

    /**
     * Set a named parameter for the given given name and String on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and String and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#VARCHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the String of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setString(String name, String value) {
        SqlParam param = new SqlParam(name, value, Types.VARCHAR);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and int on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and int and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#INTEGER}.
     *
     * @param name the name of the named parameter to set
     * @param value the int of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setInt(String name, int value) {
        SqlParam param = new SqlParam(name, value, Types.INTEGER);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and long on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and long and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#BIGINT}.
     *
     * @param name the name of the named parameter to set
     * @param value the long of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setLong(String name, long value) {
        SqlParam param = new SqlParam(name, value, Types.BIGINT);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and double on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and double and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#DOUBLE}.
     *
     * @param name the name of the named parameter to set
     * @param value the double of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setDouble(String name, double value) {
        SqlParam param = new SqlParam(name, value, Types.DOUBLE);
        return set(param);
    }

    /**
     * Set a named parameter with a <em>null</em> value for the given given name and {@link java.sql.Types sqlType} on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.Types sqlType} with a <em>null</em> value and set on
     * the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setNull(String name, int sqlType) {
        SqlParam param = new SqlParam(name, null, sqlType);
        return set(param);
    }

    /**
     * Set a named parameter with a <em>null</em> value for the given given name, {@link java.sql.Types sqlType} and typeName on the
     * {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name, {@link java.sql.Types sqlType} and typeName with a <em>null</em> value
     * and set on the {@link #paramMap parameter map}.
     *
     * @param name the name of the named parameter to set
     * @param sqlType the {@link java.sql.Types sql type} of the value
     * @param typeName the fully-qualified name of an SQL user-defined type; ignored if the parameter is not a user-defined type
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setNull(String name, int sqlType, String typeName) {
        SqlParam param = new SqlParam(name, null, sqlType, typeName);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and BigDecimal on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and BigDecimal and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#DECIMAL}.
     *
     * @param name the name of the named parameter to set
     * @param value the BigDecimal of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setBigDecimal(String name, BigDecimal value) {
        SqlParam param = new SqlParam(name, value, Types.DECIMAL);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and float on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and float and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#FLOAT}.
     *
     * @param name the name of the named parameter to set
     * @param value the float of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setFloat(String name, float value) {
        SqlParam param = new SqlParam(name, value, Types.FLOAT);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and boolean on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and boolean and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#BOOLEAN}.
     *
     * @param name the name of the named parameter to set
     * @param value the boolean of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setBoolean(String name, boolean value) {
        SqlParam param = new SqlParam(name, value, Types.BOOLEAN);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and char on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and char and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#CHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the char of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setChar(String name, char value) {
        String str = String.valueOf(value);
        SqlParam param = new SqlParam(name, str, Types.CHAR);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and byte on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and byte and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#TINYINT}.
     *
     * @param name the name of the named parameter to set
     * @param value the byte of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setByte(String name, byte value) {
        SqlParam param = new SqlParam(name, value, Types.TINYINT);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and bytes on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and bytes and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#VARBINARY}.
     *
     * @param name the name of the named parameter to set
     * @param value the bytes of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setBytes(String name, byte[] value) {
        SqlParam param = new SqlParam(name, value, Types.VARBINARY);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and short on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and short and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#SMALLINT}.
     *
     * @param name the name of the named parameter to set
     * @param value the short of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setShort(String name, short value) {
        SqlParam param = new SqlParam(name, value, Types.SMALLINT);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and Clob on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Clob and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#CLOB}.
     *
     * @param name the name of the named parameter to set
     * @param value the Clob of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setClob(String name, Clob value) {
        SqlParam param = new SqlParam(name, value, Types.CLOB);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and Clob Reader on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Clob Reader and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#CLOB}.
     *
     * @param name the name of the named parameter to set
     * @param value the Clob Reader of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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
    /**
     * Set a named parameter for the given given name and Blob on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Blob and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#BLOB}.
     *
     * @param name the name of the named parameter to set
     * @param value the Blob of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setBlob(String name, Blob value) {
        SqlParam param = new SqlParam(name, value, Types.BLOB);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and Blob InputStream on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Blob InputStream and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#BLOB}.
     *
     * @param name the name of the named parameter to set
     * @param value the Blob InputStream of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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
    /**
     * Set a named parameter for the given given name and {@link java.sql.Date} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.Date} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#DATE}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.Date} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setDate(String name, java.sql.Date value) {
        SqlParam param = new SqlParam(name, value, Types.DATE);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and {@link java.util.Date} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.util.Date} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#DATE}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.util.Date} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setDate(String name, java.util.Date value) {
        SqlParam param = new SqlParam(name, value, Types.DATE);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and {@link java.sql.Time} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.Time} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#TIME}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.Time} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setTime(String name, Time value) {
        SqlParam param = new SqlParam(name, value, Types.TIME);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and {@link java.sql.Timestamp} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.Timestamp} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#TIMESTAMP}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.Timestamp} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setTimestamp(String name, Timestamp value) {
        SqlParam param = new SqlParam(name, value, Types.TIMESTAMP);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and ascii InputStream on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and ascii InputStream and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#LONGVARCHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the ascii InputStream of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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
    /**
     * Set a named parameter for the given given name and binary InputStream on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and InputStream stream and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#LONGVARBINARY}.
     *
     * @param name the name of the named parameter to set
     * @param value the binary InputStream of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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
    /**
     * Set a named parameter for the given given name and Character Reader on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Character Reader and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#LONGVARCHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the Character Reader of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
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
    /**
     * Set a named parameter for the given given name and Ref on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Ref and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#REF}.
     *
     * @param name the name of the named parameter to set
     * @param value the Ref of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setRef(String name, Ref value) {
        SqlParam param = new SqlParam(name, value, Types.REF);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and array on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and array and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#ARRAY}.
     *
     * @param name the name of the named parameter to set
     * @param value the array of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setArray(String name, Array value) {
        SqlParam param = new SqlParam(name, value, Types.ARRAY);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and {@link java.net.URL} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.net.URL} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#DATALINK}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.net.URL} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setURL(String name, URL value) {
        SqlParam param = new SqlParam(name, value, Types.DATALINK);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and {@link java.sql.RowId} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.RowId} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#ROWID}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.RowId} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setRowId(String name, RowId value) {
        SqlParam param = new SqlParam(name, value, Types.ROWID);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and value on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and value and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#NVARCHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the value of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setNString(String name, String value) {
        SqlParam param = new SqlParam(name, value, Types.NVARCHAR);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and Character Reader on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and Character Reader and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#LONGNVARCHAR}.
     *
     * @param name the name of the named parameter to set
     * @param value the Character Reader of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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
    /**
     * Set a named parameter for the given given name and {@link java.sql.NClob} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.NClob} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#NCLOB}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.NClob} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setNClob(String name, NClob value) {
        SqlParam param = new SqlParam(name, value, Types.NCLOB);
        return set(param);
    }

    /**
     * Set a named parameter for the given given name and {@link java.sql.NClob} Reader on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.NClob} Reader and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#NCLOB}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.NClob} Reader of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
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
    /**
     * Set a named parameter for the given given name and {@link java.sql.SQLXML} on the {@link #paramMap parameter map}.
     * <p/>
     * A new {@link SqlParam} will be created for the given name and {@link java.sql.SQLXML} and set on the {@link #paramMap parameter map}.
     * <p/>
     * The {@link SqlParam#sqlType} will be set to {@link java.sql.Types#SQLXML}.
     *
     * @param name the name of the named parameter to set
     * @param value the {@link java.sql.SQLXML} of the named parameter to set
     * @return this SqlParams instance to enable chained calls
     * @throws IllegalArgumentException if the name is not defined
     */
    public SqlParams setSQLXML(String name, SQLXML value) {
        SqlParam param = new SqlParam(name, value, Types.SQLXML);
        return set(param);
    }

    /**
     * Remove the {@link SqlParam} for the given name.
     *
     * @param name the name of the SqlParam to remove
     * @return the SqlParam that was removed
     */
    public Object remove(String name) {
        return paramMap.remove(name);
    }

    /**
     * Remove all the {@link SqlParam} instances on this SqlParams instance.
     *
     * @return this SqlParams instance to enable chained calls
     */
    public SqlParams clear() {
        paramMap.clear();
        return this;
    }

    /**
     * Return the named parameters as a Set of Strings.
     *
     * @return the named parameters as a Set of Strings
     */
    public Set<String> keySet() {
        return paramMap.keySet();
    }

    /**
     * Return the named parameter values as a Collection of {@link SqlParams}.
     *
     * @return the named parameter values as a Collection of {@link SqlParams}
     */
    public Collection<SqlParam> values() {
        return paramMap.values();
    }

    /**
     * Return the named parameter entries as a Set, keyed on the parameter name and {@link SqlParam} value
     *
     * @return the named parameter entries as a Set, keyed on the parameter name and {@link SqlParam} value
     */
    public Set<Entry<String, SqlParam>> entrySet() {
        return paramMap.entrySet();
    }

}
