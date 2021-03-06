/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Simple interface for complex types to be set as statement parameters.
 *
 * <p>Implementations perform the actual work of setting the actual values. They must
 * implement the callback method {@code setValue} which can throw SQLExceptions
 * that will be caught and translated by the calling code. This callback method has
 * access to the underlying Connection via the given PreparedStatement object, if that
 * should be needed to create any database-specific objects.
 * 
 * <pre class="prettyprint">
 * SqlParams params = new SqlParams();
 * params.set("name", new SqlValue() {
 * 
 *     {@literal @}Override
 *     public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
 *
 *          // Here we can set the custom value for the given paramIndex
 *         ps.setString(paramIndex, "Steve");
 *     }
 * });
 * 
 * try {
 *     Connection conn = DriverManager.getConnection(...);
 *     PreparedStatement ps = OliveUtils.prepareStatement(conn, parsedSql, params);
 *     ResultSet rs = ps.executeQuery();
 * } catch (SQLException ex) {
 *     throw new RuntimException(ex);
 * } </pre>
 *
 * @author Juergen Hoeller
 */
public interface SqlValue {

	/**
	 * Set the SQL value on the given PreparedStatement at the paramIndex.
   *
	 * @param ps the PreparedStatement to work on
	 * @param paramIndex the index of the parameter for which we need to set the value
	 * @throws SQLException if a SQLException is encountered while setting parameter values
	 */
	void setValue(PreparedStatement ps, int paramIndex)	throws SQLException;
}