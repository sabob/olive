package za.sabob.olive.jdbc.operation;

import za.sabob.olive.jdbc.context.*;

public interface Operation<X extends Exception> {

    public void run( JDBCContext ctx ) throws X;
}
