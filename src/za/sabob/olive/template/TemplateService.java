package za.sabob.olive.template;

import java.io.*;
import java.util.*;
import za.sabob.olive.mustache.*;

public class TemplateService {

    private Mustache.Compiler templateCompiler;

    private Mustache.Formatter templateFormatter;

    private Mustache.Collector templateCollector;

    private Mustache.Escaper templateEscaper;

    private Mustache.TemplateLoader templateLoader;

//    public String executeTemplateFile( String filename, Map data ) {
//        Template template = loadCompiledTemplate( filename, data );
//        String result = executeTemplate( template, data );
//        return result;
//    }

//    public String executeTemplateFromFile( String filename, Map data ) {
//        String result = executeTemplateFile( filename, data );
//        return result;
//    }

    public String executeTemplate( String name, String content, Map data ) {
        Template template = compileTemplate( name, content );
        String result = executeTemplate( template, data );
        return result;
    }

    public String executeTemplate( String content, Map data ) {
        Template template = compileTemplate( content, data );

        String result = executeTemplate( template, data );
        return result;
    }

    public String executeTemplate( Template template, Map data ) {
        String result = template.execute( data );
        return result;

    }

//    public Template loadCompiledTemplate( String filename ) {
//
//        Map data = new HashMap();
//        return loadCompiledTemplate( filename, data );
//    }

//    public Template loadCompiledTemplate( String filename, Map data ) {
//
//        String content = loadContent( filename );
//        Template template = compileTemplate( filename, content, data );
//        return template;
//    }

    public Template compileTemplate( String name, String content ) {

        Map data = new HashMap();
        Template template = compileTemplate( name, content, data );
        return template;
    }

    public Template compileTemplate( String name, String content, Map data ) {
        // Bercause of partials, DONT cache
//        if ( getMode() == Mode.PRODUCTION ) {
//            Template template = templateMap.get( name );
//
//            if ( template != null ) {
//                return template;
//            }
//        }

        Template template = compileTemplate( content, data );

//        if ( getMode() == Mode.PRODUCTION ) {
//            templateMap.put( name, template );
//        }
        return template;
    }

    public Template compileTemplate( String content ) {
        Map data = new HashMap();
        return compileTemplate( content, data );
    }

    public Template compileTemplate( String content, Map data ) {

        Mustache.Compiler activeCompiler = getTemplateCompiler();
        Mustache.TemplateLoader activeTemplateLoader = getTemplateLoader();
        Mustache.Collector activeTemplateCollector = getTemplateCollector();
        Mustache.Formatter activeTemplateFormatter = getTemplateFormatter();
        Mustache.Escaper activeTemplateEscaper = getTemplateEscaper();

        if ( activeCompiler == null ) {
            activeCompiler = Mustache.compiler();
        }

        if ( activeTemplateLoader == null ) {
            activeTemplateLoader = createDefaultTemplateLoader( data );
        }
        activeCompiler = activeCompiler.withLoader( activeTemplateLoader );

        if ( activeTemplateCollector != null ) {
            activeCompiler = activeCompiler.withCollector( activeTemplateCollector );
        }

        if ( activeTemplateFormatter != null ) {
            activeCompiler = activeCompiler.withFormatter( activeTemplateFormatter );
        }

        if ( activeTemplateEscaper != null ) {
            activeCompiler = activeCompiler.withEscaper( activeTemplateEscaper );
        }

        Template template = activeCompiler.compile( content );

        return template;
    }

    public void setTemplateCompiler( Mustache.Compiler templateCompiler ) {
        this.templateCompiler = templateCompiler;
    }

    public Mustache.Compiler getTemplateCompiler() {
        return templateCompiler;
    }

    public Mustache.TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    public Mustache.Formatter getTemplateFormatter() {
        return templateFormatter;
    }

    public void setTemplateFormatter( Mustache.Formatter templateFormatter ) {
        this.templateFormatter = templateFormatter;
    }

    public Mustache.Collector getTemplateCollector() {
        return templateCollector;
    }

    public void setTemplateCollector( Mustache.Collector templateCollector ) {
        this.templateCollector = templateCollector;
    }

    public Mustache.Escaper getTemplateEscaper() {
        return templateEscaper;
    }

    public void setTemplateEscaper( Mustache.Escaper templateEscaper ) {
        this.templateEscaper = templateEscaper;
    }

    public void setTemplateLoader( Mustache.TemplateLoader templateLoader ) {
        this.templateLoader = templateLoader;
    }

    protected Mustache.TemplateLoader createDefaultTemplateLoader( final Map<String, String> data ) {

        Mustache.TemplateLoader defaultTemplateLoader = new Mustache.TemplateLoader() {
            @Override
            public Reader getTemplate( String name ) {
                String partial = data.get( name );
                if ( partial == null ) {
                    partial = "";
                };

                return new StringReader( partial );
            }
        };

        return defaultTemplateLoader;

    }
}


    
    

