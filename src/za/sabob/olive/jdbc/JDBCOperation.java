package za.sabob.olive.jdbc;

import java.sql.*;
import javax.sql.*;

/**
 * There are two ways to run in a transaction.
 *
 * Either using static Transaction(DataSource, Callback)
 *
 * or builder pattern
 * Transaction.on(Connection).execute(callback);
 *
 */
public class JDBCOperation {

    //final private Connection conn;
    final private DataSource ds;

    public JDBCOperation() {
        DataSourceContainer container = JDBCContext.getDataSourceContainer();
        this.ds = container.getActiveDataSource();
    }

    public JDBCOperation( DataSource ds ) {
        this.ds = ds;
    }

//    public JDBCOperation( Connection conn ) {
//        this.conn = conn;
//    }
    public static JDBCOperation on( DataSource ds ) {
//        Transactional.on( dataSource ).do( new TransactionCallback() {
//        @Override
//        public void doInTransaction() {            
//        }
//    });

        return new JDBCOperation( ds );
    }

    public void execute( OperationCallback callback ) {
        JDBCOperation( this.ds, callback );
    }

    // Don't pass in DataSource?
//    public static void Transactional( DataSource ds, TransactionCallback callback ) {
//
//        Connection conn = OliveUtils.getConnection( ds );
//        Transactional( conn, callback );
//
//    }

    public static void JDBCOperation( DataSource ds, OperationCallback callback ) {

        Connection conn = null;

        try {
            conn = JDBC.beginOperation( ds );

            callback.execute( conn );

        } catch ( Exception e ) {

            throw new RuntimeException( e );

        } finally {

            JDBC.cleanupOperation( conn );

        }
    }

    public static void main( String[] args ) {
//
//        final Connection conn = null; // get connection from Datasource
//        
//        try {
//            Connection conn = Tansaction.begin(ds);
//            PreparedStatement st = getStatement();
//            ResultSet rs = executeUpdate(ps,  new rowMapper());
//            Person person = toObject(rs, Person.class);
//            
//        } catch ( Exception e) {
//            Tansaction.rollback(conn);
//            
//        } finally {
//            Tansaction.close(ds);
//            
//        }

//        Transactional( conn, new TransactionCallback() {
//
//                       @Override
//                       public void execute()  throws Exception {
//                           //PreparedStatement ps;
//                           //PreparedStatement ps = OliveUtils.prepareStatement( conn, parsedSql, parameters );
//                           //OliveUtils.executeUpdate( conn, ps );
//                       }
//                   } );
//        Transactional(conn, () -> {
//          PreparedStatement ps = OliveUtils.prepareStatement( conn, parsedSql, parameters );
//          
//          Block (ps, () -> {
//              OliveUtils.executeUpdate( ps, newParams );
//              OliveUtils.executeUpdate( conn, ps, newParams );
//              OliveUtils.executeUpdate( conn, ps, newParams );              
//          });
//          OliveUtils.executeUpdate( conn, ps );
//        } );
    }

}
