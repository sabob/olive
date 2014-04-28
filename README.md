Olive
=====

Provides SQL utilities such as loading queries from files and named prepared statements.

## Features
* Load and cache external SQL files
* Configurable to load SQL files from classpath or web application root
* Named parameters for prepared statements
* Utilities to easily close Connections, Statements and ResultSets
* Thread safe - can be used in multithreaded environments
* No dependencies - Olive is a self-contained jar


##### Table of Contents  
[Intro] (#intro)   
[Load SQL] (#load-sql)  
[Named Parameters] (#named-parameters)  
[Custom SQL Value] (#custom-sql-value)  
[Utilities] (#utilities)  
[Usage] (#usage)   
[Standalone] (#standalone)   
[Web] (#web)   
[Mode] (#mode)   
[Build] (#build)   
<a href="http://sabob.github.io/olive/javadocs/api/index.html" target="_blank">Javadocs</a>


## Intro
<a id="intro"></a>
Olive provides common SQL and JDBC utilities to enhance JDBC usage. Olive doesn't replace JDBC in any way.

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
    PreparedStatement ps = olive.prepareStatement(conn, sql, params);

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

## Load SQL
<a id="load"></a>
The primary use case for Olive is to load and cache external SQL files in order to create JDBC Statements with.

While it is possible to write SQL strings in Java code, it is cumbersome, especially large queries spanning multiple lines where each line has to be concatenated. It also makes it difficult to execute the query in our favorite query tool because we need remove the Java String concatenations.

If the SQL files are externalized we can just copy and paste the queries from and to our query tools.

Olive use <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ResourceLoaders.html" target="_blank">ResourceLoaders</a> to load SQL files with.

Olive ships with two resource loaders, <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ClasspathResourceLoader.html" target="_blank">ClasspathResourceLoader</a> which is the default and
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/WebappResourceLoader.html" target="_blank">WebappResourceLoader</a>.

ClasspathResourceLoader is used to load SQL files from the classpath, while WebappResourceLoader is used to load SQL files from the web application root folder in servlet containers.

In PRODUCTION mode, the default mode, Olive will cache all loaded files for fast retrieval in the future. In DEVELOPMENT mode Olive does not perform any caching, and changes to files are picked up automatically.

## Named Parameters
<a id="named"></a>
JDBC provides a PreparedStatement for writing queries which automatically escapes the values which also ensures SQL injection cannot occur.

However PreparedStatement uses index based parameters which is cumbersome to match when working with large queries which change over time as you continuously need to adjust the index positions.

Named parameters are an alternative where instead of using question marks ('?') and indexes we name the parameters and use this name to specify it's value, instead of an index position.

_Note:_ Olive does not replace the PreparedStatement, it simply provides an alternative way to create PreparedStatements from a SQL string.

_Also note:_ Named parameters feature is based on the <a href="http://projects.spring.io/spring-framework/" target="_blank">Spring framework</a>, although Olive does not depend on Spring at all.

When Olive parses a query (by calling Olive.loadParsedSql or Olive.prepareStatement), it automatically finds all named parameters and replaces them with '?'. A named parameter is specified as a ':' followed by an identifier, for example:

`select.sql`:
```sql
SELECT * FROM mytable WHERE name = :name and age >= :age
```

In the query above two named parameters are specified namely _:name_ and _:age_. By invoking Olive.loadParsedSql("select.sql"), Olive will load and parse the SQL. Parsing basically means Olive will scan for all named parameters, mark where they occur in the SQL, and replace them a '?'. The above query becomes:

```sql
SELECT * FROM mytable WHERE name = ? and age >= ?
```

Since Olive marked the occurrences of the named parameters it knows that the 1st '?' is where the :name parameter should be used and the 2nd parameter is for :age.

To specify the values for the named parameters Olive provides the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/SqlParams.html" target="_blank">SqlParams</a> class.

SqlParams is a HashMap but with an API similar to PreparedStatement in that you can specify the value type with methods such as `setString(String name, String value)`, `setInt(String name, int value)`, `setBoolean(String name, boolean bool)` etc. Belows is an example for specifying the named parameters for the query above:

```java
ParsedSql sql = olive.loadParsedSql("myfile.sql");
SqlParams params = new SqlParams();
params.setString("name", "Bob");
params.setInt("age", 18);
```

## Custom SQL Value
<a id="custom-sql-value"></a>
When using named parameters for PreparedStatements it is sometimes neccessary to set a parameter such as a custom Java type. In these situations we can create a <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/SqlValue.html" target="_blank">SqlValue</a> instance and set that as the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/SqlParam.html" target="_blank">SqlParam</a> value.

SqlValue is an interface with a single method, <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/SqlValue.html#setValue(java.sql.PreparedStatement, int)" target="_blank">setValue</a> for setting a PreparedStatement parameter at the given index.

Say we have the following Money class to pass as a named parameter:

```java
public vclass Money {
    private int dollars;
    private int sents;
    
    // getters and setters
}
```

To use Money.java as a named parameter we will create a custom SqlValue instance to set the Money object as a PreparedStatement parameter: 

```java

// $3.45
Money money = new Money(3, 45);

SqlValue moneySqlValue = new SqlValue() {

    @Override
    public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
        String val = money.getDollars() + "|" + money.getSents();
        ps.setString(paramIndex, val);
    }
};

SqlParams params = new SqlParams();
params.set("money", moneySqlValue);
```

Above we create a SqlValue and set it as value of the named parameter "money". In the _setValue_ method we create a String representation of our money class and set that as the PreparedStatement parameter.
## Utilities
<a id="utilities"></a>
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html" target="_blank">OliveUtils</a> provides common SQL utilities such as:

* easily <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#close(java.sql.Connection)" target="_blank">closing</a> resources without _try/catch_ and _null_ checking logic neccessary.
* <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#normalize(java.lang.Class, java.lang.String)" target="_blank">normalize</a> paths to SQL files
* create <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#prepareStatement(java.sql.Connection, za.sabob.olive.ps.ParsedSql, za.sabob.olive.ps.SqlParams)" target="_blank">prepareStatements</a> from SQL files containing named parameters
* <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#setParams(java.sql.PreparedStatement, za.sabob.olive.ps.ParsedSql, za.sabob.olive.ps.SqlParams)" target="_blank">set named parameter values</a> on existing PreparedStatements


## Usage
<a id="usage"></a>
The most common components of Olive are the classes 
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/Olive.html" target="_blank">Olive</a>,
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ResourceLoader.html" target="_blank">ResourceLoader</a>,
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/SqlParams.html" target="_blank">SqlParams</a> and
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html" target="_blank">OliveUtils</a>

<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/Olive.html" target="_blank">Olive</a> provides the main entry for loading, parsing and caching external SQL files.

Creating an instance of Olive is easy, simply create a new instance with one of it's many constructors.

_Note:_ Olive is _thread safe_ so a single instance  can be created and shared in a multi threaded environment such as a servlet container.

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

Olive can be customized where it loads SL files from by specifying which
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ResourceLoader.html" target="_blank">ResourceLoader</a> to use.

Olive ships with two resource loaders, <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ClasspathResourceLoader.html" target="_blank">ClasspathResourceLoader</a> which is the default and
<a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/WebappResourceLoader.html" target="_blank">WebappResourceLoader</a> which is outlined in the [web] (#web) section.

ClasspathResourceLoader loads SQL file form the classpath. We must provide the _absolute_ path to the SQL files. Absolute paths starts with a _'/'_ character. For example, given the sql file:

`org/mycorp/dao/person/insert_person.sql`:

We use the following to find and parse the SQL file:

```java
ClasspathResourceLoader loader = new ClasspathResourceLoader();
Olive olive = new Olive(loader);
ParsedSql sql = olive.loadParsedSql("/org/mycorp/dao/person/insert_person.sql");

```

However, we can use the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#normalize(java.lang.Class, java.lang.String)" target="_blank">OliveUtils.normalize(Class, filename)</a> to create absolute filenames relative to the given class argument. For example:

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
ParsedSql parsedSql = olive.loadParsedSql(fullname);
```

When Olive loads SQL files it will cache the result for fast retrieval in the future. Olive also caches the parsed SQL results so there is no need to reparse for future queries.

Note: in development mode Olive does not perform any caching.

Olive provides named parameters for easily authoring queries for PreparedStatements. In order to find the named parameter in a SQL string Olive parses (and caches) the string.

Olive.loadParsedSql returns a <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/ParsedSql.html" target="_blank">ParsedSql</a> instance which contains the information about where each named parameter is located in the SQL.

You can query ParsedSql about the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/ParsedSql.html#getOriginalSql()" target="_blank">Original SQL</a> as well as the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/ParsedSql.html#getParameterNames()" target="_blank">names</a>
 and <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/ParsedSql.html#getParameterIndexes()" target="_blank">location</a> of the named parameters. You can also see the number of <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/ParsedSql.html#getNamedParameterCount()" target="_blank">named</a> and <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/ParsedSql.html#getUnnamedParameterCount()" target="_blank">unnamed</a> parameters in the SQL string.

To specify the named parameters to use for the SQL we use the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/ps/SqlParams.html" target="_blank">SqlParams</a>  class.

SqlParams provides a similar API to PreparedStatement for setting parameters, but instead of using indexes it uses names. For example we can set the :name and :age named parameters as follows:

```java
ParsedSql sql = olive.loadParsedSql("myfile.sql");
SqlParams params = new SqlParams();
params.setString("name", "Bob");
params.setInt("age", 18);
```

Equipped with the ParsedSql and SqlParams we can create the PreparedStatement:

```java

Connection conn;
PreparedStatement ps;
ResultSet rs;

try {
    Connection conn...
    Olive olive = new Olive();
    ParsedSql sql = olive.loadParsedSql("myfile.sql");
    SqlParams params = new SqlParams();
    ps = olive.prepareStatement(conn, sql, params);
    rs = ps.execute();
} catch (SQLException e) {
    throw new RuntimeException(e);
} finally {
    OliveUtils.close(conn, ps, rs);
}
```



Note: above we wrap the SQLException as a RuntimeException and rethrow it. It is common to have a centralized exception handling mechanism to catch any errors occuring in the code. For example in a standalone application the Thread.setUncaughtExceptionHandler is often used. In a web app an exception handling Filter is often used to log and alert errors.

In the example above we use <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/util/OliveUtils.html#close(java.sql.ResultSet, java.sql.Statement, java.sql.Connection)" target="_blank">OliveUtils.close</a> in the finally block to close the Connection, PreparedStatement and ResultSet. OliveUtils.close will safely handle null values for any of these resources and any exceptions thrown by closing these resources will be rethrown as a RuntimeException.

Olive also supports named parameters for SELECT IN type queries. For example:

```sql
SELECT * FROM person p WHERE p.id IN (1, 3, 5, 10)
```

We can create this query with named parameters as follows:

```sql
SELECT * FROM person p WHERE p.id IN (:ids)
```

By specifying _:ids_ as a collection of primitives, it will be expanded to a '?' for each item in the collection:

```java
Conn conn = ...
ParsedSql sql = ...
SqlParams params = new SqlParams();
List list = new ArrayList();
list.add(1);
list.add(3);
list.add(5);
list.add(10);
params.set("ids", list); 
PreparedStatement ps = olive.createStatement(conn, sql, params);
```

WARNING:_ the maximum number of entries in the collection should not exceed 100. The JDBC spec does not guarantee that the PreparedStatement will work for larger number of entries, although some drivers could support it.

In addition to primitives _:ids_ could also be a collection of collections or a collection of arrays. This allows for queries such as:

```sql
SELECT * FROM person p WHERE (p.id, p.name) IN ( (1, 'John'), (3, 'Steve'))
```

This is dependent of wether or not the database supports such queries.

## Standalone
<a id="standalone"></a>
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

To use this in our application we can use:
```java
Olive olive = AppUtils.getOlive();
String name =  OliveUtils.normalize(PersonDao.class, "insert_person.sql");
ParsedSql sql = olive.loadParsedSql(name);
```

## Web
<a id="web"></a>
In a web environment such as a Servlet container, we should rather use the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/WebappResourceLoader.html" target="_blank">WebapResourceLoader</a>  instead of the <a href="http://sabob.github.io/olive/javadocs/api/za/sabob/olive/loader/ClasspathResourceLoader.html" target="_blank">ClasspathResourceLoader</a>. The problem with the ClasspathResourceLoader is that when making changes
to the SQL files it could cause the container to restart, which isn't ideal in development mode, where we often make changes to the files.

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
<a id="mode"></a>
As seen above Olive has a PRODUCTION and DEVELOPMENT mode as well as a TRACE mode. 

PRODUCTION mode is the default mode and ensures that SQL files that are loaded are cached for fast retrieval in the future. Once the SQL file is parsed the result is also cached so files do not have to be reparsed each time.

DEVELOPMENT mode is useful while developing our applications as the SQL files are reloaded each time. Changes made to the files are immediately visible.

TRACE mode is useful when we want to see output printed as to why errors occur.

When using Spring profiles it is common to leverage the _spring.profiles.active_ property in web.xml to setup the 
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
<a id="build"></a>
