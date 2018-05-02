package za.sabob.olive.jdbc2.operation;

import za.sabob.olive.jdbc2.context.*;

public interface OperationUpdater<X extends Exception> {

    public void update( JDBCContext ctx ) throws X;
}
