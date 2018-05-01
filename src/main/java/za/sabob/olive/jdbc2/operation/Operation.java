package za.sabob.olive.jdbc2.operation;

import za.sabob.olive.jdbc2.context.*;

public interface Operation {

    public void doOperation( JDBCContext ctx ) throws Exception;
}
