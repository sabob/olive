package za.sabob.olive.transaction.sync;

public class SynchronizedJDBC {

    private static final ThreadLocal<JDBCContainer> HOLDER = new ThreadLocal<JDBCContainer>();

    public static JDBCContainer getJDBCContainer() {

        JDBCContainer container = HOLDER.get();

        if ( container == null ) {
            container = new JDBCContainer();
            bindJDBCContainer( container );
        }
        
        return container;
    }

    public static boolean hasJDBCContainer() {
        return HOLDER.get() != null;
    }

    public static void bindJDBCContainer( JDBCContainer container ) {
        HOLDER.set( container );
    }

    public static void unbindJDBCContainer() {
        HOLDER.set( null );
    }

}
