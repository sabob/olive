package za.sabob.olive.jdbc2.transaction;

import za.sabob.olive.jdbc2.context.*;

public interface TransactionExecutor<T, X extends Exception> {

    public T execute( JDBCContext ctx ) throws X;
}
