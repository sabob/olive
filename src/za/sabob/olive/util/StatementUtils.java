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

import za.sabob.olive.ps.SqlParam;
import za.sabob.olive.Mode;
import za.sabob.olive.Olive;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

/**
 * Utility methods for setting the values of a {@link java.sql.PreparedStatement}.
 *
 * <p>
 * Used by {@link OliveUtils}
 * but also available for direct use in custom creation and setup of a {@link java.sql.PreparedStatement}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 */
class StatementUtils {

    private static final Logger LOGGER = Logger.getLogger(StatementUtils.class.getName());

    static final Set<String> driversWithNoSupportForGetParameterType = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(1));

    private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<Class<?>, Integer>(32);

    static {
        /* JDBC 3.0 only - not compatible with e.g. MySQL at present
         javaTypeToSqlTypeMap.put(boolean.class, new Integer(Types.BOOLEAN));
         javaTypeToSqlTypeMap.put(Boolean.class, new Integer(Types.BOOLEAN));
         */
        javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
        javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
        javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
        javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
        javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
        javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
        javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
        javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
        javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
        javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
        javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
        javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
        javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
        javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
        javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
        javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
    }

    /**
     * Derive a default SQL type from the given Java type.
     * @param javaType the Java type to translate
     * @return the corresponding SQL type, or {@code null} if none found
     */
    public static int javaTypeToSqlParameterType(Class<?> javaType) {
        Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
        if (sqlType != null) {
            return sqlType;
        }
        if (Number.class.isAssignableFrom(javaType)) {
            return Types.NUMERIC;
        }
        if (isStringValue(javaType)) {
            return Types.VARCHAR;
        }
        if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
            return Types.TIMESTAMP;
        }
        return OliveUtils.TYPE_UNKNOWN;
    }

    /**
     * Set the value for a parameter. The method used is based on the SQL type
     * of the parameter and we can handle complex types like arrays and LOBs.
     * @param ps the prepared statement or callable statement
     * @param paramIndex index of the parameter we are setting
     * @param inValue the value to set
     * @throws SQLException if thrown by PreparedStatement methods
     */
    public static void setParameterValue(PreparedStatement ps, int paramIndex,  SqlParam inValue) throws SQLException {

        setParameterValueInternal(ps, paramIndex, inValue);
    }

    /**
     * Set the value for a parameter. The method used is based on the SQL type
     * of the parameter and we can handle complex types like arrays and LOBs.
     * @param ps the prepared statement or callable statement
     * @param paramIndex index of the parameter we are setting
     * @param sqlType the SQL type of the parameter
     * @param typeName the type name of the parameter
     * (optional, only used for SQL NULL and SqlTypeValue)
     * @param scale the number of digits after the decimal point
     * (for DECIMAL and NUMERIC types)
     * @param inValue the value to set (plain value or a SqlTypeValue)
     * @throws SQLException if thrown by PreparedStatement methods
     * @see SqlTypeValue
     */
    private static void setParameterValueInternal(PreparedStatement ps, int paramIndex, SqlParam inValue) throws SQLException {

        String typeNameToUse = inValue.getTypeName();
        int sqlTypeToUse = inValue.getSqlType();
        Object inValueToUse = inValue.getValue();
        Integer scale = inValue.getScale();

        // override type info?
        if (Olive.getMode() == Mode.TRACE) {
            LOGGER.info("Overriding type info with runtime info from SqlParam: name [" + inValue.getName() + "], column index " + paramIndex + ", SQL type "
                + inValue.getSqlType() + ", type name " + inValue.getTypeName());
        }
        if (sqlTypeToUse != OliveUtils.TYPE_UNKNOWN) {
            sqlTypeToUse = inValue.getSqlType();
        }

         if (inValue.getTypeName() != null) {
         typeNameToUse = inValue.getTypeName();
         }

        if (Olive.getMode() == Mode.TRACE) {
            LOGGER.info("Setting SQL statement parameter value: name [" + inValue.getName() + "], column index " + paramIndex + ", parameter value [" + inValueToUse
                + "], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") + "], SQL type " + (sqlTypeToUse
                == OliveUtils.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
        }

        if (inValueToUse == null) {
            setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
        } else {
            setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
        }
    }

    /**
     * Set the specified PreparedStatement parameter to null,
     * respecting database-specific peculiarities.
     */
    private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException {
        if (sqlType == OliveUtils.TYPE_UNKNOWN) {
            boolean useSetObject = false;
            Integer sqlTypeToUse = null;
            DatabaseMetaData dbmd = null;
            String jdbcDriverName = null;
            boolean checkGetParameterType = true;
            if (!driversWithNoSupportForGetParameterType.isEmpty()) {
                try {
                    dbmd = ps.getConnection().getMetaData();
                    jdbcDriverName = dbmd.getDriverName();
                    checkGetParameterType = !driversWithNoSupportForGetParameterType.contains(jdbcDriverName);
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Could not check connection metadata", ex);
                }
            }
            if (checkGetParameterType) {
                try {
                    sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
                } catch (Throwable ex) {
                    if (Olive.getMode() == Mode.TRACE) {
                        LOGGER.log(Level.INFO, "JDBC 3.0 getParameterType call not supported - using fallback method instead: ", ex);
                    }
                }
            }
            if (sqlTypeToUse == null) {
                // JDBC driver not compliant with JDBC 3.0 -> proceed with database-specific checks
                sqlTypeToUse = Types.NULL;
                try {
                    if (dbmd == null) {
                        dbmd = ps.getConnection().getMetaData();
                    }
                    if (jdbcDriverName == null) {
                        jdbcDriverName = dbmd.getDriverName();
                    }
                    if (checkGetParameterType) {
                        driversWithNoSupportForGetParameterType.add(jdbcDriverName);
                    }
                    String databaseProductName = dbmd.getDatabaseProductName();
                    if (databaseProductName.startsWith("Informix") || jdbcDriverName.startsWith("Microsoft SQL Server")) {
                        useSetObject = true;
                    } else if (databaseProductName.startsWith("DB2") || jdbcDriverName.startsWith("jConnect") || jdbcDriverName.startsWith(
                        "SQLServer") || jdbcDriverName.startsWith("Apache Derby")) {
                        sqlTypeToUse = Types.VARCHAR;
                    }
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Could not check connection metadata", ex);
                }
            }
            if (useSetObject) {
                ps.setObject(paramIndex, null);
            } else {
                ps.setNull(paramIndex, sqlTypeToUse);
            }
        } else if (typeName != null) {
            ps.setNull(paramIndex, sqlType, typeName);
        } else {
            ps.setNull(paramIndex, sqlType);
        }
    }

    private static void setValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
        Integer scale, Object inValue) throws SQLException {

        if (inValue instanceof SqlValue) {
            ((SqlValue) inValue).setValue(ps, paramIndex);
        } else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR || (sqlType == Types.CLOB && isStringValue(inValue.getClass()))) {
            ps.setString(paramIndex, inValue.toString());
        } else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
            if (inValue instanceof BigDecimal) {
                ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
            } else if (scale != null) {
                ps.setObject(paramIndex, inValue, sqlType, scale);
            } else {
                ps.setObject(paramIndex, inValue, sqlType);
            }
        } else if (sqlType == Types.DATE) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Date) {
                    ps.setDate(paramIndex, (java.sql.Date) inValue);
                } else {
                    ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.DATE);
            }
        } else if (sqlType == Types.TIME) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Time) {
                    ps.setTime(paramIndex, (java.sql.Time) inValue);
                } else {
                    ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.TIME);
            }
        } else if (sqlType == Types.TIMESTAMP) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Timestamp) {
                    ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
                } else {
                    ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
            }
        } else if (sqlType == OliveUtils.TYPE_UNKNOWN) {
            if (isStringValue(inValue.getClass())) {
                ps.setString(paramIndex, inValue.toString());
            } else if (isDateValue(inValue.getClass())) {
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
            } else {
                // Fall back to generic setObject call without SQL type specified.
                ps.setObject(paramIndex, inValue);
            }
        } else {
            // TODO add blob clob etc?
            // Fall back to generic setObject call with SQL type specified.
            ps.setObject(paramIndex, inValue, sqlType);
        }
    }

    /**
     * Check whether the given value can be treated as a String value.
     */
    private static boolean isStringValue(Class<?> inValueType) {
        // Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
        return (CharSequence.class.isAssignableFrom(inValueType) || StringWriter.class.isAssignableFrom(inValueType));
    }

    /**
     * Check whether the given value is a {@code java.util.Date}
     * (but not one of the JDBC-specific subclasses).
     */
    private static boolean isDateValue(Class<?> inValueType) {
        return (java.util.Date.class.isAssignableFrom(inValueType) && !(java.sql.Date.class.isAssignableFrom(inValueType)
            || java.sql.Time.class.isAssignableFrom(inValueType) || java.sql.Timestamp.class.isAssignableFrom(inValueType)));
    }

    /**
     * Clean up all resources held by parameter values which were passed to an
     * execute method. This is for example important for closing LOB values.
     * @param paramValues parameter values supplied. May be {@code null}.
     */
    public static void cleanupParameters(Object[] paramValues) {
        if (paramValues != null) {
            cleanupParameters(Arrays.asList(paramValues));
        }
    }

    /**
     * Clean up all resources held by parameter values which were passed to an
     * execute method. This is for example important for closing LOB values.
     * @param paramValues parameter values supplied. May be {@code null}.
     */
    public static void cleanupParameters(Collection<?> paramValues) {
        if (paramValues != null) {
            for (Object inValue : paramValues) {
                if (inValue instanceof DisposableSqlValue) {
                    ((DisposableSqlValue) inValue).cleanup();
                }
            }
        }
    }

}
