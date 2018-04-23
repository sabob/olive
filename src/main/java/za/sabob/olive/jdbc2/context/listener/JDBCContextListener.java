package za.sabob.olive.jdbc2.context.listener;

import za.sabob.olive.jdbc2.context.*;

public abstract class JDBCContextListener {

    public void onConnectionClosed( JDBCContext ctx ) {
    }

    public void onBeforeContextDetach( JDBCContext ctx ) {
    }
}
