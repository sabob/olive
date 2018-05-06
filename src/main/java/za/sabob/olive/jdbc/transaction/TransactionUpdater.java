package za.sabob.olive.jdbc.transaction;

import za.sabob.olive.jdbc.context.*;

public interface TransactionUpdater<X extends Exception> {

    public void update( JDBCContext ctx ) throws X;
}
