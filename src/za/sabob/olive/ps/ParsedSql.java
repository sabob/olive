/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a parsed SQL statement.
 * <p/>
 * You can retrieve the {@link #originalSql original SQL string}, find {@link #getParameterNames() named parameters} and other useful
 * information about the named parameters.
 * 
 * <pre class="prettyprint">
 * SqlParams params = new SqlParams();
 * params.setString("name", "Steve");
 * params.setInt("age", 21);
 * 
 * ParsedSql parsedSql = olive.loadParsedSql("/sql/person/insert-person.sql");
 * 
 * int namedParameterCount = parsedSql.getNamedParameterCount();
 * 
 * // We check if the number of named parameters in the SQL string matches our input SqlParams.
 * if (params.size() != namedParameterCount) {
 *  throw new IllegalStateException("The named parameters in the SQL statement does not match the number of input parameters!");
 * }
 * </pre>
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class ParsedSql {

    /** The original sql string before being parsed. */
    private String originalSql;

    /** List of named parameters in the SQL string. */
    private List<String> parameterNames = new ArrayList<String>();

    /** List of indexes (start and end) of the named parameters in the SQ: string. */
    private List<int[]> parameterIndexes = new ArrayList<int[]>();

    /** The number of named parameters in the SQL string. */
    private int namedParameterCount;

    /** The number of unnamed parameters (question marks '?') in the SQL string. */
    private int unnamedParameterCount;

    /** The total number of named and unnamed parameters (question marks '?') in the SQL string. */
    private int totalParameterCount;

    /**
     * Create a new ParsedSql instance for the given SQL string.
     * @param originalSql the SQL statement to be parsed
     */
    public ParsedSql(String originalSql) {
        this.originalSql = originalSql;
    }

    /**
     * Return the SQL statement that was parsed.
     *
     * @return the original SQL that was parsed
     */
    public String getOriginalSql() {
        return this.originalSql;
    }

    /**
     * Return all of the named parameters in the parsed SQL statement.
     * Repeated occurrences of the same parameter name are included here.
     *
     * @return list of parameter names found in the SQL statement
     */
    public List<String> getParameterNames() {
        return this.parameterNames;
    }

    /**
     * Return the named parameter indexes for the specified parameter position.
     *
     * @param parameterPosition the position of the parameter (as index in the parameter names List)
     * @return the start index and end index, combined into a int array of length 2
     */
    public int[] getParameterIndexes(int parameterPosition) {
        return this.parameterIndexes.get(parameterPosition);
    }

    /**
     * Return all the named parameter indexes in the parsed SQL string.
     * 
     * @return all the named parameter indexes in the parsed SQL string
     */
    public List<int[]> getParameterIndexes() {
        return this.parameterIndexes;
    }

    /**
     * Return the count of named parameters in the SQL statement.
     * <p/>
     * Each parameter name counts once; repeated occurrences do not count here.
     * 
     * @see #getUnnamedParameterCount() 
     * @see #getTotalParameterCount() 
     *
     * @return the number of named parameters found in the SQL statement
     */
    public int getNamedParameterCount() {
        return this.namedParameterCount;
    }

    /**
     * Return the count of all of the unnamed parameters (question marks '?') in the SQL statement.
     * 
     * @see #getNamedParameterCount()
     * @see #getTotalParameterCount()
     *
     * @return the number of unnamed parameters (question marks '?') found in the SQL statement
     */
    public int getUnnamedParameterCount() {
        return this.unnamedParameterCount;
    }

    /**
     * Return the total count of all of the parameters in the SQL statement.
     * <p/>
     * Repeated occurrences of the same parameter name do count here.
     * 
     * @see #getNamedParameterCount()
     * @see #getUnnamedParameterCount() 
     *
     * @return the total number of all parameters found in the SQL statement
     */
    public int getTotalParameterCount() {
        return this.totalParameterCount;
    }

    /**
     * Add a named parameter parsed from this SQL statement.
     * @param parameterName the name of the parameter
     * @param startIndex the start index in the original SQL String
     * @param endIndex the end index in the original SQL String
     */
    public void addNamedParameter(String parameterName, int startIndex, int endIndex) {
        this.parameterNames.add(parameterName);
        this.parameterIndexes.add(new int[] { startIndex, endIndex });
    }

    /**
     * Set the count of named parameters in the SQL statement.
     * <p/>
     * Each parameter name counts once; repeated occurrences do not count here.
     *
     * @param namedParameterCount the count of named parameters in the SQL statement
     */
    public void setNamedParameterCount(int namedParameterCount) {
        this.namedParameterCount = namedParameterCount;
    }

    /**
     * Set the count of all of the unnamed parameters in the SQL statement.
     *
     * @param unnamedParameterCount the count of all of the unnamed parameters in the SQL statement
     */
    public void setUnnamedParameterCount(int unnamedParameterCount) {
        this.unnamedParameterCount = unnamedParameterCount;
    }

       /**
     * Set the total count of all of the parameters in the SQL statement.
     * <p/>
     * Repeated occurrences of the same parameter name do count here.
     * 
     * @param totalParameterCount the total count of all of the parameters in the SQL statement
     */
    public void setTotalParameterCount(int totalParameterCount) {
        this.totalParameterCount = totalParameterCount;
    }

    /**
     * Exposes the original SQL String.
     *
     * @return the original SQL String
     */
    @Override
    public String toString() {
        return this.originalSql;
    }

}
