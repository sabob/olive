package za.sabob.olive.jdbc2.stack;

import za.sabob.olive.jdbc.*;
import za.sabob.olive.jdbc2.JDBCContext;

public class JDBCContextStack {

    private final Stack<JDBCContext> contextList = new Stack();

    public JDBCContext getMostRecentContext() {
        return peekTop();
    }

    public void add( JDBCContext ctx ) {
        JDBCContext mostRecent = getMostRecentContext();


        if ( mostRecent != null ) {
            mostRecent.attach( ctx );
        }

        contextList.add( ctx );
    }

    public JDBCContext peekTop() {
        return contextList.peekTop();
    }

    public JDBCContext pop() {
        return contextList.pop();
    }

    public boolean isEmpty() {
        return contextList.isEmpty();
    }
    
    public int size() {
        return contextList.size();
    }
    
    @Override
    public String toString() {
        return contextList.toString();
    }
}
