package za.sabob.olive.jdbc;

@FunctionalInterface
public interface OperationCallback {
    
    public void execute( JDBCContext ctx ) throws Exception;

}
