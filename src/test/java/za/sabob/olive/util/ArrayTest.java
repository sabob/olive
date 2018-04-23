package za.sabob.olive.util;

import java.util.*;
import za.sabob.olive.ps.*;

public class ArrayTest {

    public static void main( String[] args ) {

        Object value = new long[][]{ {},{1, 2}};
        List list = OliveUtils.toList(  value );
        System.out.println( "list: " + list );


        String sql = "select * from test where id in (:ids)";
        ParsedSql parsedSql = OliveUtils.parseSql( sql );

        SqlParams params = new SqlParams();

        params.set( "ids", list );
        String newSql = OliveUtils.substituteNamedParameters( parsedSql, params );
        System.out.println( "NewSql " + newSql );

    }

}
