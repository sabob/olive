package za.sabob.olive.jdbc.operation;

import za.sabob.olive.jdbc.context.*;

public interface OperationUpdater<X extends Exception> {

    public void update( JDBCContext ctx ) throws X;
}
