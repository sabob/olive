package za.sabob.olive.jdbc.operation;

import za.sabob.olive.jdbc.context.*;

public interface OperationExecutor<T, X extends Exception> {

    public T execute( JDBCContext ctx ) throws X;
}
