package za.sabob.olive.jdbc.daoDone;

public class TryWithResource {

    static class Foo implements AutoCloseable {
        
        public Foo() {
            //throw new RuntimeException("CTOR");
        }

        public void close() {
            throw new RuntimeException("bad close");
        }
    }

    public static void main( final String[] args ) {
        try ( Foo f = new Foo() ) {
            
            System.out.println( "No op!" );
            throw new RuntimeException("bad body");
            
        }
    }
}
