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

    public String execute( String name, String content, Map data ) {
        Template template = compile( name, content );
        String result = execute( template, data );
        return result;
    }

    public String execute( String content, Map data ) {
        Template template = compile( content, data );

        String result = execute( template, data );
        return result;
    }

    public String execute( Template template, Map data ) {
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

    public Template compile( String name, String content ) {

        Map data = new HashMap();
        Template template = compile( name, content, data );
        return template;
    }

    public Template compile( String name, String content, Map data ) {
        // Bercause of partials, DONT cache
//        if ( getMode() == Mode.PRODUCTION ) {
//            Template template = templateMap.get( name );
//
//            if ( template != null ) {
//                return template;
//            }
//        }

        Template template = compile( content, data );

//        if ( getMode() == Mode.PRODUCTION ) {
//            Cache.getInstance().putTemplate( name, template );
//        }
        return template;
    }

    public Template compile( String content ) {
        Map data = new HashMap();
        return compile( content, data );
    }

    public Template compile( String content, Map data ) {

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





