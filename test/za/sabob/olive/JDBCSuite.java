package za.sabob.olive;

import java.util.*;
import java.util.logging.*;
import org.testng.*;
import org.testng.xml.*;
import za.sabob.olive.jdbc2.*;
import za.sabob.olive.jdbc2.threads.JDBCThreadedTest;

public class JDBCSuite {

    private final static Logger LOGGER = Logger.getLogger( JDBCSuite.class.getName() );

    //@BeforeSuite
    public static void main( String[] args ) {
        TestNG testNG = new TestNG();

        ITestNGListener tla = new MyListener();

        //listnerClasses.add( org.wso2.platform.test.core.PlatformTestManager.class );
        //listnerClasses.add( org.wso2.platform.test.core.PlatformSuiteManager.class );
        testNG.setDefaultSuiteName( "JDBC suite" );

        XmlPackage xmlPackage = new XmlPackage( "za.sabob.olive.jdbc2.*" );
        //xmlPackage.setInclude(Arrays.asList( "*.*" ) );
        List<XmlPackage> xmlPackages = new ArrayList<>();
        xmlPackages.add( xmlPackage );

        XmlSuite suite = new XmlSuite();
        XmlTest test = new XmlTest( suite );
        test.setXmlPackages( xmlPackages );

        List<XmlSuite> xmlSuites = new ArrayList<>();
        xmlSuites.add( suite );

        testNG.setXmlSuites( xmlSuites );

        //runClasses( testNG );

        testNG.addListener( tla );
        testNG.run();
        
        try {
            //Thread.sleep(999999999);
        } catch ( Exception ex ) {
            Logger.getLogger( JDBCSuite.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    public static class MyListener extends TestListenerAdapter {

        Set<String> unique = new HashSet<>();

        @Override
        public void onTestStart( ITestResult result ) {
            String combo = result.getInstanceName() + " " + result.getName();

            if ( unique.contains( combo ) ) {
                return;
            }

            unique.add( combo );
            System.out.println( "Running test : " + result.getInstance().getClass().getSimpleName() + " " + result.getName() );
        }

        @Override
        public void onConfigurationFailure( ITestResult result ) {
            log( result );
        }

        @Override
        public void onTestFailedButWithinSuccessPercentage( ITestResult result ) {
            log( result );
        }

        @Override
        public void onTestFailure( ITestResult result ) {
            log( result );
        }

        private void log( ITestResult result ) {
            Throwable t = result.getThrowable();
            LOGGER.log( Level.SEVERE, "Name=" + result.getName() + " testName=" + result.getTestName() + ", Status=" + result.getStatus(), t );

        }
    }

    private static void runClasses( TestNG testNG ) {
        testNG.setTestClasses( new Class[] {
            JDBCInsertTest.class,
            JDBCTest.class,
            JDBCThreadedTest.class
        } );
    }

}
