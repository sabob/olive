package za.sabob.olive.loader;

import za.sabob.olive.Mode;
import za.sabob.olive.config.OliveConfig;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.jdbc.ps.ParsedSql;
import za.sabob.olive.util.Cache;
import za.sabob.olive.util.OliveUtils;

import java.io.InputStream;

public class ResourceService {

    protected ResourceLoader resourceLoader;

    public ResourceService() {
        this( new ClasspathResourceLoader() );
    }

    public ResourceService( ResourceLoader loader ) {
        this.resourceLoader = loader;
    }


    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public void setResourceLoader( ResourceLoader resourceLoader ) {
        this.resourceLoader = resourceLoader;
    }

    public String loadContent( String path ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "path cannot be null!" );
        }

        if ( OliveConfig.getMode() == Mode.PRODUCTION ) {
            String  content = Cache.getInstance().getFileContent( path );
            if ( content != null ) {
                return content;
            }
        }

        InputStream is = getResourceLoader().getResourceStream( path );

        String file = OliveUtils.toString( is );

        if ( OliveConfig.getMode() == Mode.PRODUCTION ) {
            Cache.getInstance().putFileContent( path, file );
        }
        return file;
    }

    // TODO move this to a higher abstract utility eg Olive.class
    public ParsedSql loadParsedSql( String path ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "path cannot be null!" );
        }

        if ( OliveConfig.getMode() == Mode.PRODUCTION ) {
            ParsedSql parsedSql = Cache.getInstance().getParsedSql( path );

            if ( parsedSql != null ) {
                return parsedSql;
            }
        }

        String sql = loadContent( path );
        ParsedSql parsedSql = JDBCUtils.parseSql( sql );

        if ( OliveConfig.getMode() == Mode.PRODUCTION ) {
            Cache.getInstance().putParsedSql( path, parsedSql );
        }

        return parsedSql;
    }
}
