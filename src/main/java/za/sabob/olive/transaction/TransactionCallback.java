package za.sabob.olive.transaction;

import za.sabob.olive.jdbc.*;

public interface TransactionCallback {

    public void execute( JDBCContext ctx ) throws Exception;

}
