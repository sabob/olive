package za.sabob.olive.jdbc.transaction;

import za.sabob.olive.jdbc.context.*;

public interface TransactionExecutor<T, X extends Exception> {

    public T execute( JDBCContext ctx ) throws X;
}
