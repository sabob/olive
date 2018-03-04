package za.sabob.olive.transaction;

import java.sql.*;

/**
 * There are two ways to run in a transaction.
 * 
 * Either using static Transaction(Connection, Callback)
 * 
 * or builder pattern
 * Transaction.on(Connection).execute(callback);
 * 
 */
public class Transactional {

    final private Connection conn;

    public Transactional( Connection conn ) {
        this.conn = conn;
    }

    public static Transactional on( Connection conn ) {
//        Transactional.on(conn).do( new TransactionCallback() {
//        @Override
//        public void doInTransaction() {            
//        }
//    });

        return new Transactional( conn );
    }

    public void execute( TransactionCallback callback ) {
        Transactional( this.conn, callback );
    }

    // Don't pass in DataSource?
//    public static void Transactional( DataSource ds, TransactionCallback callback ) {
//
//        Connection conn = OliveUtils.getConnection( ds );
//        Transactional( conn, callback );
//
//    }

    public static void Transactional( Connection conn, TransactionCallback callback ) {

        try {
            // TODO bind the conn to a THREAD_LOCAL so that OliveUtils can grab the conn!
            TX.beginTransaction( conn );

            callback.execute();

            TX.commitTransaction(  conn );

        } catch ( Exception e ) {
            TX.rollbackTransactionAndThrow(conn, e );
        } finally {
            TX.cleanupTransaction( conn );
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
