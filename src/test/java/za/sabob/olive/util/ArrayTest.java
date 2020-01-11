package za.sabob.olive.util;

import java.util.*;
import za.sabob.olive.jdbc.ps.*;
import za.sabob.olive.jdbc.util.JDBCUtils;

public class ArrayTest {

    public static void main( String[] args ) {

        Object value = new long[][]{ {},{1, 2}};
        List list = JDBCUtils.toList(  value );
        System.out.println( "list: " + list );


        String sql = "select * from test where id in (:ids)";
        ParsedSql parsedSql = JDBCUtils.parseSql( sql );

        SqlParams params = new SqlParams();

        params.set( "ids", list );
        String newSql = JDBCUtils.substituteNamedParameters( parsedSql, params );
        System.out.println( "NewSql " + newSql );

    }

}
