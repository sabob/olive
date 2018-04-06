package za.sabob.olive.jdbc2;

import java.util.*;
import javax.sql.*;
import za.sabob.olive.jdbc2.stack.*;

public class DataSourceContainer {

    final private Map<DataSource, JDBCContextPipeline> pipelinesByDS = new LinkedHashMap<>();

    public JDBCContext createTXContext( DataSource ds ) {
        return createContext( ds, true );
    }

    public JDBCContext createContext( DataSource ds ) {
        return createContext( ds, false );
    }

    public JDBCContext getMostRecentContext( DataSource ds ) {
        JDBCContextPipeline pipeline = getOrCreatePipeline( ds );
        return pipeline.getMostRecentContext();
    }

    protected JDBCContext createContext( DataSource ds, boolean transactional ) {
        JDBCContextPipeline pipeline = getOrCreatePipeline( ds );

        JDBCContext ctx = pipeline.createContext( ds, transactional );
        return ctx;
    }

//
//    public void deleteContext( JDBCContext ctx ) {
//        ctx.detach( );
//    }
    public boolean isEmpty() {
        return pipelinesByDS.isEmpty();
    }

    public boolean isEmpty( DataSource ds ) {
        JDBCContextPipeline pipeline = getPipeline( ds );
        if ( pipeline == null ) {
            return true;
        }

        return pipeline.isEmpty();
    }

    protected JDBCContextPipeline getOrCreatePipeline( DataSource ds ) {

        JDBCContextPipeline pipeline = getPipeline( ds );

        if ( pipeline == null ) {
            JDBCFactory.getInstance().createPipeline();
            pipeline = new JDBCContextPipeline();
            pipelinesByDS.put( ds, pipeline );
        }

        return pipeline;
    }

    protected JDBCContextPipeline getPipeline( DataSource ds ) {
        return pipelinesByDS.get( ds );
    }

}
