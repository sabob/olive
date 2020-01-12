package za.sabob.olive.jdbc.basic;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import za.sabob.olive.Mode;
import za.sabob.olive.Olive;
import za.sabob.olive.config.OliveConfig;
import za.sabob.olive.jdbc.JDBC;
import za.sabob.olive.jdbc.JDBCContext;
import za.sabob.olive.jdbc.ps.ParsedSql;
import za.sabob.olive.jdbc.ps.SqlParams;
import za.sabob.olive.jdbc.util.JDBCUtils;
import za.sabob.olive.loader.ClasspathResourceLoader;
import za.sabob.olive.loader.ResourceLoader;
import za.sabob.olive.loader.ResourceService;
import za.sabob.olive.postgres.PostgresBaseTest;
import za.sabob.olive.postgres.PostgresTestUtils;
import za.sabob.olive.query.RowMapper;
import za.sabob.olive.template.TemplateService;
import za.sabob.olive.util.OliveUtils;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class BestPracticeJDBCTest extends PostgresBaseTest {

    @BeforeClass( alwaysRun = true )
    public void beforeThisClass() {
        PostgresTestUtils.populateDatabase( ds );
    }

    @Test
    public void bestPracticeTest2() {

        // This test shows how to load a SQL statement from a file and perform the SQL operation.
        // No Exception handling or try/catch logic need to be declared. The Connection, Statement and ResultSet will be closed automatically

        OliveConfig.setMode( Mode.DEVELOPMENT );

        ResourceLoader loader = new ClasspathResourceLoader();
        ResourceService resourceService = new ResourceService( loader );

        String path = "BestPracticeJDBCTest.sql";
        String normalizedPath = OliveUtils.normalize( path, getClass() );


        String sql = resourceService.loadContent( normalizedPath );
        //String sql = resourceService.loadFile();
        //String sql = resourceService.loadAndParseSql(); // BAD we dont want to do two things in one go at such low level
        ParsedSql parsedSql = JDBCUtils.parseSql( sql );


        TemplateService templateService = new TemplateService();

        Map data = new HashMap<>();
        String result = templateService.execute( sql, data );

        //resourceService.loadFile
        //resourceService.loadSql
        //resourceService.loadTemplate

        //JDBC - Which one below?
//        JDBCUtils.prepareStatementFromFile;
//        PS.fromFile;
//        JDBC.prepareStatementFromFile;

//        RS.toBeans;
//        JDBCUtils.toBeans;

        JDBCContext ctx1 = null;
        try {

        } catch ( Exception ex ) {
            ctx1.rollback();
            JDBC.rollbackTransactionAndThrow( ctx1, ex );
        } finally {
            JDBC.cleanupTransaction( ctx1 );
        }

        JDBC.inOperation( ds, ( ctx ) -> {

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );

            String filename = "BestPracticeJDBCTest.sql";
            String testContent = Olive.loadContent( filename );

            RowMapper<String> mapper = ( rs1, rowNum ) -> rs1.getString( 0 );
            String str = Olive.executeFileToBean( ctx, filename, params, data, mapper );

            PreparedStatement ps = JDBCUtils.prepareStatement( ctx, testContent, params ); // PreparedStatement is added to JDBCContext to close automatically
            String name = JDBCUtils.mapToPrimitive( String.class, ps ); // The underlying ResultSet will be closed automatically

            String path2 = OliveUtils.normalize( "BestPracticeJDBCTest.sql", getClass() );
            String sql2 = resourceService.loadContent( path );
            ParsedSql parsed = JDBCUtils.parseSql( sql2 );
            ps = JDBCUtils.prepareStatement( ctx, parsed, params ); // PreparedStatement is added to JDBCContext to close automatically
            name = JDBCUtils.mapToPrimitive( String.class, ps ); // The underlying ResultSet will be closed automatically
            Assert.assertEquals( name, "bob" );
        } );

        JDBC.inOperation( ds, ( ctx ) -> {

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );

            String path2 = OliveUtils.normalize( "BestPracticeJDBCTest.sql", getClass() );
            String sql2 = resourceService.loadContent( path );
            ParsedSql parsed = JDBCUtils.parseSql( sql2 );
            PreparedStatement ps = JDBCUtils.prepareStatement( ctx, parsed, params ); // PreparedStatement is added to JDBCContext to close automatically
            String name = JDBCUtils.mapToPrimitive( String.class, ps ); // The underlying ResultSet will be closed automatically
            Assert.assertEquals( name, "bob" );
        } );

    }

    @Test
    public void bestPracticeTest() {

        ResourceLoader loader = new ClasspathResourceLoader();
        ResourceService resourceService = new ResourceService( loader );

        // This test shows how to load a SQL statement from a file and perform the SQL operation.
        // No Exception handling or try/catch logic need to be declared. The Connection, Statement and ResultSet will be closed automatically

        Olive olive = new Olive( Mode.DEVELOPMENT );

        JDBC.inOperation( ds, ( ctx ) -> {

            SqlParams params = new SqlParams();
            params.set( "name", "bob" );
            String path = OliveUtils.normalize( "BestPracticeJDBCTest.sql", getClass() );
            resourceService.loadContent( path );
            PreparedStatement ps = JDBCUtils.prepareStatement( ctx, path, params ); // PreparedStatement is added to JDBCContext to close automatically
            String name = JDBCUtils.mapToPrimitive( String.class, ps ); // The underlying ResultSet will be closed automatically
            Assert.assertEquals( name, "bob" );
        } );
    }
}
