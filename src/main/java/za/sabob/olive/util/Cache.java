package za.sabob.olive.util;

import za.sabob.olive.mustache.Template;
import za.sabob.olive.jdbc.ps.ParsedSql;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    protected static Cache INSTANCE = new Cache();

    protected Map<String, String> fileMap = new ConcurrentHashMap<>();

    protected Map<String, ParsedSql> parsedSqlMap = new ConcurrentHashMap<>();

    public static Cache getInstance() {
        return INSTANCE;
    }

    public String getFileContent( String name ) {
        String content = getFileMap().get( name );
        return content;
    }

    public void putFileContent( String name, String content ) {
        getFileMap().put( name, content );
    }


    public void removeFileContent( String name ) {
        getFileMap().remove( name );
    }

    public ParsedSql getParsedSql( String name ) {
        ParsedSql parsedSql = getParsedSqlMap().get( name );
        return parsedSql;
    }

    public void putParsedSql( String name, ParsedSql parsedSql ) {
        getParsedSqlMap().put( name, parsedSql );
    }

    public void removeParsedSql( String name ) {
        getParsedSqlMap().remove( name );
    }

    public void remove( String name ) {
        getFileMap().remove( name );
        getParsedSqlMap().remove( name );
    }

    public void clear() {
        getFileMap().clear();
        getParsedSqlMap().clear();
    }


    private Map<String, String> getFileMap() {
        return fileMap;
    }

    private Map<String, ParsedSql> getParsedSqlMap() {
        return parsedSqlMap;
    }
}
