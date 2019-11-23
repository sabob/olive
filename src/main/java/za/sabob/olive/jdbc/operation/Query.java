package za.sabob.olive.jdbc.operation;

import za.sabob.olive.jdbc.JDBCContext;

public interface Query<T, X extends Exception> {

    public T get( JDBCContext ctx ) throws X;
}
