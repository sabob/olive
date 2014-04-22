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
    
    // Load the sql file. By default Olive uses a ClassPath resource loader so we specify the
    // absolute path to the file
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

_Note:_ Olive is thread safe so a single instance  can be created and shared in a multi threaded environment such as a servlet environment.

```java
// By default Olive starts in PRODUCTION mode and automatically caches SQL files it loads
// Olive also uses the ClasspathResourceLoader by default to load SQL files on the classpath
Olive olive = new Olive();

// Specify DEVELOPMENT mode to ensure SQL files are reloaded each time
Olive olive = new Olive(Mode.DEVELOPMENT);

// Specify the WebappResourceLoader to load SQL files from the web root. Useful in Servlet environments
WebappResourceLoader loader = new WebappResourceLoader(servletContext);
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
Using Olive in a standalone application it is often desirable to access Olive as a singleon. Since Olive is thread safe this can easily be achieved as follows:

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

Note that the WebappResourceLoader needs access to the <a href="http://docs.oracle.com/javaee/5/api/javax/servlet/ServletContext.html" target="_blank">ServletContext</a>.

An easy way to access the ServletContext and setup a single Olive instance is through <a href="http://docs.oracle.com/javaee/5/api/javax/servlet/ServletContextListener.html" target="_blank">ServletContextListener</a>. See example below:

```java
public class OliveStartupListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String appMode = AppUtils.getAppMode();
        WebappResourceLoader loader = new WebappResourceLoader(servletContext);
        AppUtils.createOlive(appMode, loader);
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
    }

}

public class AppUtils {
    private static Olive OLIVE;
    private static String appMod;
    
    public static Olive createOlive(String appMode, ResourceLoader loader) {
        if (olive != null) {
            throw new IllegalStateException("Olive was already created!");
        }

        olive = new Olive(loader);
        if ("prod".equals(appMode)) {
            olive.setMode(za.sabob.olive.Mode.PRODUCTION);

        } else {
            olive.setMode(za.sabob.olive.Mode.DEVELOPMENT);
        }

        return olive;
    }
    
      public static String getMode(ServletContext servletContext) {
        if (appMode == null) {
            // Load application mode(dev, prod etc.)  from a web.xml context parameter
            String appMode = servletContext.getInitParameter("app-mode");
            return appMode;

        }
        return appMode;
    }
}
```

The web.xml would look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app id="WebApp_ID" version="2.5" xmlns="http://java.sun.com/xml/ns/javaee"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">

    <context-param>
        <param-name>app-mode</param-name>
        <param-value>dev</param-value>
    </context-param>
    
    ...
    
</web-app>
```

With the WebAppResourceLoader in place we can store our SQL files in the web app root folder for example:

> webapp/sql/person/_person.sql  
> webapp/sql/person/insert_person.sql  
> webapp/sql/person/update_person.sql

Olive can now be retrieved as follows in our web app:
```java
Olive olive = AppUtils.getOlive();
ParsedSql sql = olive.loadParsedSql("/sql/person/insert_peson.sql");
```

Note: in a web application it doesn't make sense to use OliveUtils.normalize since the SQL files are not relative to class files, but are placed on the webapp folder instead.

## Mode
<a id="#mode"></a>
As seen above Olive has a PRODUCTION and DEVELOPMENT mode as well as a TRACE mode. 

PRODUCTION mode is the default mode and ensures that SQL files that are loaded are cached for fast retrieval in the future. Once the SQL file is parsed the result is also cached so files do not have to be reparsed each time.

DEVELOPMENT mode is useful while developing your application as the SQL files are reloaded each time. Changes made to the files are immediately visible.

TRACE mode is useful when you want to see output printed as to why errors occur.

If you are using Spring profiles it is common to leverage the _spring.profiles.active_ property in web.xml to setup the 
mode. For example, given the following web.xml:

The web.xml would look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app id="WebApp_ID" version="2.5" xmlns="http://java.sun.com/xml/ns/javaee"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">

    <context-param>
        <param-name>spring.profiles.active</param-name>
        <param-value>dev</param-value>
    </context-param>
    
    ...
    
</web-app>
```

The following Java snippet could be used to setup Olive's mode:

```java
public class AppUtils {
    
    private static String appMode;
    
    public static Mode getOliveMode(ServletContext servletContext) {
    
        Mode oliveMode = null;
        
        String appMode = getAppMode(servletContext);
        if ("prod".equals(appMode)) {
            oliveMode = Mode.PRODUCTION;
        } else {
            oliveMode = Mode.DEVELOPMENT;
        }

        return oliveMode;
    }
    
    public static String getAppMode(ServletContext servletContext) {
        if (appMode == null) {
        
            // Use Spring's spring.profiles.active property as the application mode
            appMode = servletContext.getInitParameter(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
        }
        return appMode;
    }
}
```

## Build
<a id="#build"></a>
