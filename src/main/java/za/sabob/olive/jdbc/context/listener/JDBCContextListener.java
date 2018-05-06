package za.sabob.olive.jdbc.context.listener;

import za.sabob.olive.jdbc.context.*;

public abstract class JDBCContextListener {

    public void onConnectionClosed( JDBCContext ctx ) {
    }

    public void onBeforeContextDetach( JDBCContext ctx ) {
    }
}
