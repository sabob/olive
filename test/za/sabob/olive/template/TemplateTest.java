package za.sabob.olive.template;

import java.io.*;
import java.util.*;
import org.testng.annotations.*;
import za.sabob.olive.*;
import za.sabob.olive.mustache.*;
import za.sabob.olive.ps.*;
import static za.sabob.olive.util.OliveUtils.path;

public class TemplateTest {

    @Test
    public void testTemplate() {
        Olive olive = new Olive();

        Mustache.Compiler compiler = Mustache.compiler();

        olive.getTemplateService().setTemplateCompiler( compiler );

        SqlParams params = new SqlParams();
        params.set( "country", "RSA" );
        params.set( "code", "123" );

        Map data = params.toMap();

        Map child = new HashMap();
        child.put( "pok", "moo.pok" );
        data.put( "moo", child );
        data.put( "myPartial", "{{moo.pok}}" );
        //data.put("hasWhere", new HashMap().size() > 0);
        data.put("hasWhere", data.size() > 0);

        Mustache.Lambda batch = new Mustache.Lambda() {
            public void execute( Template.Fragment frag, Writer out ) throws IOException {

                String batch = frag.execute( );
                System.out.println( "BATCH: " + batch );
            }
        };
        
        
//        
//        Mustache.Formatter formatter = new Mustache.Formatter() {
//            @Override
//            public String format( Object value ) {
//                return "MOO: " + value;
//            }
//        };
        
        //olive.getTemplateService().setTemplateFormatter( formatter );

        data.put( "batch", batch );

String path = path( this, "sql/template.sql");
        String result = olive.executeTemplateFile( path, data );

        System.out.println(
            "Result: " + result );
    }

}
