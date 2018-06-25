package za.sabob.olive.jdbc.operation;

import za.sabob.olive.jdbc.context.*;

public interface Query<T, X extends Exception> {

    public T get( JDBCContext ctx ) throws X;
}
