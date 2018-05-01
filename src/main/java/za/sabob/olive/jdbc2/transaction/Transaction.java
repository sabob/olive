package za.sabob.olive.jdbc2.transaction;

import za.sabob.olive.jdbc2.context.*;

public interface Transaction {

    public void doInTransaction( JDBCContext ctx ) throws Exception;
}
