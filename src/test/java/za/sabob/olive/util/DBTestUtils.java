package za.sabob.olive.util;

public class DBTestUtils {

    public static boolean isTimeout( Throwable ex ) {
        if ( ex.getMessage().contains( "Login timeout" ) || ex.getMessage().contains( "Invalid argument in JDBC call" )
            || ex.getMessage().contains( "connection does not exist" ) ) {
            return true;
        }
        return false;
    }

}
