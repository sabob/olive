package za.sabob.olive.jdbc.transaction;

import za.sabob.olive.jdbc.JDBCContext;

@FunctionalInterface
public interface TransactionalOperation<X extends Exception> {

    public void run( JDBCContext ctx ) throws X;
}
