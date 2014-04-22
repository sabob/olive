Olive
=====

SQL utilities such as loading queries from files and named prepared statements.

##### Table of Contents  
[Intro] (#intro)   
[Usage] (#usage)   
[Standalone] (#standalone)   
[Web] (#web)   
[Mode] (#mode)   
[Build] (#build)   
<a href="http://sabob.github.io/olive/javadocs/api/index.html" target="_blank">Javadocs</a>


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
The most common components of Olive are the classes 
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/Olive.html" target="_blank">Olive</a>,
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ResourceLoader.html" target="_blank">ResourceLoader</a>,
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/SqlParams.html" target="_blank">SqlParams</a> and
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html" target="_blank">OliveUtils</a>

<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/Olive.html" target="_blank">Olive</a> provides the main entry for loading, parsing and caching external SQL files.

Creating an instance of Olive is easy, simply create a new instance with one of the many constructors.

```java
// By default Olive starts in PRODUCTION mode and automatically caches SQL files it loads
// Olive also uses the ClasspathResourceLoader by default to load SQL files on the classpath
Olive olive = new Olive();

// Specify DEVELOPMENT mode to ensure SQL files are reloaded each time
Olive olive = new Olive(Mode.DEVELOPMENT);

// Specify the WebappResourceLoader to load SQL files from the web root. Useful in Servlet environments
WebappResourceLoader loader = new WebappResourceLoader(servletContext)
Olive olive = new Olive(loader);

```

Olive can be customized where it loads SL files from by specifying the
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ResourceLoader.html" target="_blank">ResourceLoader</a> to use.

The <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ClasspathResourceLoader.html" target="_blank">ClasspathResourceLoader</a> loads SQL file form the classpath. You must provide the _absolute_ path to the SQL files. Absolute paths starts with a _'/'_ character. For example, given the sql file:

`org/mycorp/dao/person/insert_person.sql`:

We use the following to find and parse the SQL file:

```java
ClasspathResourceLoader loader = new ClasspathResourceLoader();
Olive olive = new Olive(loader);
ParsedSql sql = olive.loadParsedSql("/org/mycorp/dao/person/insert_person.sql");

```

However, you can use the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#normalize-java.lang.Class-java.lang.String-" target="_blank">OliveUtils.normalize(Class, filename)</a> to create absolute filenames relative to the given class argument. For example:

```java
import org.mycorp.dao.person.PersonDao;

String fullname = OliveUtils.normalize(PersonDao.class, "insert_person.sql");
// fullname will be "/org/mycorp/dao/person/insert_person.sql"

// normalize also handles relative paths, for example if we want to navigate from the PersonDao class
// to the product folder to load the insert_product.sql file we can use the following:
fullname = OliveUtils.normalize(PersonDao.class, "../product/insert_product.sql");
// fullname will be "/org/mycorp/dao/person/insert_product.sql"

// We can then use this normalized name to load SQL files with Olive
Olive olive = new Olive();
String fullname = OliveUtils.normalize(PersonDao.class, "insert_person.sql");
String parsedSql = olive.loadParsedSql(fullname);
```

## Standalone
<a id="#standalone"></a>
Using Olive in a standalone application it is often desirable to access Olive as a singleon. Since Olive is thread safe this can easily be done as follows:

```java
public class AppUtils {

    private static Olive OLIVE;

    public static Olive getOlive() {
        if (OLIVE == null) {
            
            
            Mode oliveMode = null;

            String appMode = getAppMode();
            if ("dev".equals(appMode) {
                oliveMode = Mode.DEVELOPMENT;
                
            } else {
                oliveMode = Mode.PRODUCTION;
            }
            
            OLIVE = new Olive(mode);
        }
        return OLIVE;
    }
    
    public static String getAppMode() {
        // Load application mode(dev, prod etc.)  which could be stored in a properties file
        String mode = ResourceBundle.getBundle("myapp").getString("mode");
        return mode;
    }
}

```

To use this in your application you can use:
```java
Olive olive = AppUtils.getOlive();
String name =  OliveUtils.normalize(PersonDao.class, "insert_person.sql");
ParsedSql sql = olive.loadParsedSql(name);
```

## Web
<a id="#web"></a>
In a web environment such as a Servlet container, we should rather use the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/WebappResourceLoader.html" target="_blank">WebapResourceLoader</a>  instead of the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ClasspathResourceLoader.html" target="_blank">ClasspathResourceLoader</a>. The problem with the ClasspathResourceLoader is that when making changes
to the SQL files it could cause the container to restart, which isn't ideal in development mode, where you often make changes to the files.

To use the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/WebappResourceLoader.html" target="_blank">WebapResourceLoader</a> create Olive as follows:

```java
WebappResourceLoader loader = new WebappResourceLoader(servletContext);
Olive olive = new Olive(loader);
```

Note that the WebappResourceLoader needs access to the ServletContext. 


## Mode
<a id="#mode"></a>

## Build
<a id="#build"></a>
