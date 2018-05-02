package za.sabob.olive.jdbc2.operation;

import za.sabob.olive.jdbc2.context.*;

public interface OperationExecutor<T, X extends Exception> {

    public T execute( JDBCContext ctx ) throws X;
}
