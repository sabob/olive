olive
=====

SQL utilities such as loading queries from files and named prepared statements.

##### Table of Contents  
[Intro] (#intro)   
[Usage] (#usage)   
[Standalone] (#standalone)   
[Web] (#web)   
[Mode] (#mode)   
[Build] (#build)   

## Intro
<a id="#intro"></a>
Below is a sample SQL file we want to load into our application.

`insert_person.sql`:
```sql
/* Insert statement */
INSERT INTO PERSON (name, age) VALUES (:name, :age);
```

Note the named parameters _:name_ and _:age_.

Below we use Olive to load this SQL file and create a PreparedStatement passing the named parameters we want to use.

`test.java`:
```java
try {
    // Retrieve a connection
    Connection conn = ...;
    
    // Create an instance of olive
    Olive olive  = new Olive();
    
    // Load the sql file. By default Olive uses a ClassPath resource loader so we specify the absolute path to the file
    ParsedSql sql = olive.loadParsedSql("/org/mycorp/dao/person/insert_person.sql");
    
    // Setup the named parameters for :name and :age
    SqlParams params = new SqlParams();
    params.setString("name", "Steve Sanders");
    params.setInt("age", 21);
    
    // Create a PreparedStatement for the given sql and parameters
    PreparedStatement ps = OliveUtils.prepareStatement(conn, sql, params);

    // Execute the statement to receive the ResultSet
    ResultSet rs = pstmt.executeQuery();
    ...

} catch (SQLException e) {
    throw new RuntimeException(e);

} finally {
    
    // We close all JDBC resources
    OliveUtils.close(rs, pstmt, conn);
}
```

## Usage
<a id="#usage"></a>

## Standalone
<a id="#standalone"></a>

## Web
<a id="#web"></a>

## Mode
<a id="#mode"></a>

## Build
<a id="#build"></a>
