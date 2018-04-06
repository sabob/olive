package za.sabob.olive.jdbc2.stack;

import java.util.*;

public class Stack<E> {

    private final List<E> list = new ArrayList<>();

    public void add( E e ) {
        list.add( e );
    }

    public boolean remove( E conn ) {
        return list.remove( conn );

    }

    public E pop() {
        if ( list.isEmpty() ) {
            return null;
        }
        return list.remove( list.size() - 1 );
    }

    public E peekTop() {
        if ( list.isEmpty() ) {
            return null;
        }

        int start = list.size() - 1;
        E e = list.get( start );

        return e;
    }

    public E peekBottom() {
        if ( list.isEmpty() ) {
            return null;
        }

        return list.get( 0 );
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }

    public boolean isAtRoot() {
        return list.size() == 1;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + ", size: " + list.size();
    }
}
