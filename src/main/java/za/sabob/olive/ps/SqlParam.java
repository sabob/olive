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

import java.io.*;
import za.sabob.olive.util.*;

/**
 * Provides a class for setting a single named parameter.
 * <p/>
 * This class is used by {@link SqlParams} to define the named parameters to populate a {@link java.sql.PreparedStatement} with.
 * <p/>
 * Example usage:
 * <pre class="prettyprint">
 * SqlParams params = new SqlParams();
 * SqlParam param = new SqlParam("age", 21);
 * params.set(param);
 * param = new SqlParam("name", "Steve);
 * params.set(param);
 *
 * // Inernally SqlParams creates SqlParam instances
 * params.setInt("code", 123");
 * </pre>
 *
 * SqlParam also contains all the data found on {@link java.sql.PreparedStatement#setObject(int, java.lang.Object) PreparedStatement.setXXX}
 * methods.
 * <p/>
 * The data represented by SqlParams are:
 * <ul>
 * <li>{@link #name} - the SQL parameter name</li>
 * <li>{@link #value} - the SQL parameter value</li>
 * <li>{@link #sqlType} - the {@link java.sql.Types type} of the parameter</li>
 * <li>{@link #scale} - the number of digits after the decimal point. Used for decimal parameter</li>
 * <li>{@link #typeName} - the fully-qualified name of an SQL user-defined type; ignored if the parameter is not a user-defined type
 * or {@link java.sql.Types#REF}</li>
 * </ul>
 */
public class SqlParam implements Serializable {

    private static long serialVersionUID = 1L;

    /**
     * The named parameter value.
     */
    private Object value;

    /**
     * The named parameter name.
     */
    private String name;

    /**
     * The named parameter {@link java.sql.Types type}.
     */
    private int sqlType = OliveUtils.TYPE_UNKNOWN;

    /**
     * The named parameter scale or length - the number of digits after the decimal.
     */
    private Integer scale;

    /**
     * The named parameter fully-qualified name of an SQL user-defined type; ignored if the parameter is not a user-defined type
     * or {@link java.sql.Types#REF}.
     */
    private String typeName;

    /**
     * Creates a new default SqlParam instance.
     */
    public SqlParam() {
    }

    /**
     * Creates a new default SqlParam instance for the give name and value.
     * <p/>
     * The name of the SqlParam should correspond to a name in the named parameter in a SQL statement.
     *
     * @param name the name of the SqlParam
     * @param value the value of the SqlParam
     */
    public SqlParam(String name, Object value) {
        this.name = name;
        setValueAndConvertArrayToList( value );
    }

    /**
     * Creates a new default SqlParam instance for the give name, value and sqlType.
     * <p/>
     * The name of the SqlParam should correspond to a name in the named parameter in a SQL statement.
     *
     * @param name the name of the SqlParam
     * @param value the value of the SqlParam
     * @param sqlType the {@link java.sql.Types type} of the value
     */
    public SqlParam(String name, Object value, int sqlType) {
        this.name = name;
        setValueAndConvertArrayToList( value );
        this.sqlType = sqlType;
    }

    /**
     * Creates a new default SqlParam instance for the give name, value, sqlType and scale.
     * <p/>
     * The name of the SqlParam should correspond to a name in the named parameter in a SQL statement.
     * <p/>
     * Scale is used if the value is a Decimal number.
     *
     * @param name the name of the SqlParam
     * @param value the value of the SqlParam
     * @param sqlType the {@link java.sql.Types type} of the value
     * @param scale the number of digits after the decimal in case value is a Decimal number
     */
    public SqlParam(String name, Object value, int sqlType, Integer scale) {
        this.name = name;
        setValueAndConvertArrayToList( value );
        this.sqlType = sqlType;
        this.scale = scale;
    }

    /**
     * Creates a new default SqlParam instance for the give name, value, sqlType and typeName.
     * <p/>
     * The name of the SqlParam should correspond to a name in the named parameter in a SQL statement.
     * <p/>
     * typeName is the named parameter fully-qualified name of an SQL user-defined type; ignored if the parameter is not a user-defined type
     * or {@link java.sql.Types#REF}.
     * 
     * @param name the name of the SqlParam
     * @param value the value of the SqlParam
     * @param sqlType the {@link java.sql.Types type} of the value
     * @param typeName the named parameter fully-qualified name of an SQL user-defined type
     */
    public SqlParam(String name, Object value, int sqlType, String typeName) {
        this.name = name;
        setValueAndConvertArrayToList( value );
        this.sqlType = sqlType;
        this.typeName = typeName;
    }

    /**
     * Return the name of the SqlParam.
     * 
     * @return the name of the SqlParam
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the SqlParam.
     * 
     * @param name the name of the SqlParam
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the value of the SqlParam.
     * 
     * @return the value of the SqlParam
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the value of the SqlParam.
     * 
     * @param value the value of the SqlParam
     */
    public void setValue(Object value) {

        setValueAndConvertArrayToList( value );

    }

    /**
     * Return the {@link java.sql.Types sql type} of the SqlParam.
     * 
     * @return the sqlType of the SqlParam
     */
    public int getSqlType() {
        return sqlType;
    }

    /**
     * Set the {@link java.sql.Types sql type} of the SqlParam.
     * 
     * @param sqlType the sqlType of the SqlParam
     */
    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    /**
     * Return the scale of the SqlParam. Scale is the number of digits after the decimal point.
     * 
     * @return the scale of the SqlParam
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * Set the scale of the SqlParam.
     * 
     * @param scale the scale of the SqlParam
     */
    public void setScale(Integer scale) {
        this.scale = scale;
    }

    /**
     * Return the typeName of the SqlParam.
     * 
     * @return the typeName the type name of the SqlParam
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Set the typeName of the SqlParam.
     * 
     * @param typeName the typeName of the SqlParam
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    /**
     * Set the value of the SqlParam and convert the value to a list if it is an array.
     * 
     * @param value the value of the SqlParam
     */
    private void setValueAndConvertArrayToList(Object value) {

        if ( OliveUtils.isArray( value ) ) {
            value = OliveUtils.toList( value );
        }

        this.value = value;
    }
}
