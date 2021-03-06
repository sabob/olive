package za.sabob.olive.jdbc.transaction;

import za.sabob.olive.jdbc.context.*;

@FunctionalInterface
public interface TransactionalQuery<T, X extends Exception> {

    public T get( JDBCContext ctx ) throws X;
}
