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

import java.util.*;
import za.sabob.olive.util.*;

/**
 *
 */
public class NamedSqlParser {

      /**
     * Parses a query with named parameters. The parameter-index mappings are
     * put into the map, and the
     * parsed query is returned. DO NOT CALL FROM CLIENT CODE. This
     * method is non-private so JUnit code can
     * test it.
     * @param query query to parse
     * @param indexMap map to hold parameter-index mappings
     * @return the parsed query
     */
    public static final String parse(String query, Map<String, List<Integer>> indexMap) {
        
        if (OliveUtils.isBlank(query)) {
            throw new IllegalArgumentException("query is required!");
        }
        
        if (indexMap == null) {
            throw new IllegalArgumentException("indexMap is required!");
        }
        
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inTSQLQuote = false;
        boolean inMySQLQuote = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        for (int i = 0; i < length; i++) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }

            } else if (inMySQLQuote) {
                if (c == '`') {
                    inMySQLQuote = false;
                }

            } else if (inTSQLQuote) {
                if (c == ']') {
                    inTSQLQuote = false;
                }

            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '`') {
                    inMySQLQuote = true;
                } else if (c == '[') {
                    inTSQLQuote = true;
                } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end if the parameter

                    List<Integer> indexList = indexMap.get(name);
                    if (indexList == null) {
                        indexList = new LinkedList<Integer>();
                        indexMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }
            parsedQuery.append(c);
        }

        // replace the lists of Integer objects with arrays of ints
        /*
        for (Iterator itr = indexMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            List list = (List) entry.getValue();
            int[] indexes = new int[list.size()];
            int i = 0;
            for (Iterator itr2 = list.iterator(); itr2.hasNext();) {
                Integer x = (Integer) itr2.next();
                indexes[i++] = x;
            }
            entry.setValue(indexes);
        }*/

        return parsedQuery.toString();
    }
    
    public static void main(String[] args) {
        String query = "Hello ':world' \":wold\" :world :middle :world";
        Map indexMap = new HashMap();
                
        String result = NamedSqlParser.parse(query, indexMap);
        System.out.println(result);
        System.out.println(indexMap);
    }
}
