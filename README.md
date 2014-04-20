olive
=====

SQL utilities such as loading queries from files and named prepared statements.


```java

Connection conn = ...;
Olive olive  = new Olive();
ParsedSql sql = olive.loadParsedSql("insert_person.sql");
PreparedStatement ps = DBUtils.createStatement(conn, sql);
```
