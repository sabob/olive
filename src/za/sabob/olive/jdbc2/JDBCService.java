package za.sabob.olive.jdbc2;

import za.sabob.olive.jdbc2.stack.JDBCContextStack;

public class JDBCService {

    private static final ThreadLocal<JDBCContextStack> HOLDER = new ThreadLocal<JDBCContextStack>();


}
