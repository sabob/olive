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

/**
 *
 */
public class SqlParam implements Serializable {

    private static long serialVersionUID = 1L;

    private Object value;

    private String name;

    private int sqlType = OliveUtils.TYPE_UNKNOWN;

    private Integer scale;

    private String typeName;

    public SqlParam() {
    }

    public SqlParam(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public SqlParam(String name, Object value, int sqlType) {
        this.name = name;
        this.value = value;
        this.sqlType = sqlType;
    }

    public SqlParam(String name, Object value, int sqlType, Integer scale) {
        this.name = name;
        this.value = value;
        this.sqlType = sqlType;
        this.scale = scale;
    }

    public SqlParam(String name, Object value, int sqlType, String typeName) {
        this.name = name;
        this.value = value;
        this.sqlType = sqlType;
        this.typeName = typeName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * @return the sqlType
     */
    public int getSqlType() {
        return sqlType;
    }

    /**
     * @param sqlType the sqlType to set
     */
    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    /**
     * @return the typeName
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @param typeName the typeName to set
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
