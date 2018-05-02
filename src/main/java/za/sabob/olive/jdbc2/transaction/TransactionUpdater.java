package za.sabob.olive.jdbc2.transaction;

import za.sabob.olive.jdbc2.context.*;

public interface TransactionUpdater<X extends Exception> {

    public void update( JDBCContext ctx ) throws X;
}
