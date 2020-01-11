/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.sabob.olive;

import za.sabob.olive.loader.LoadTest;
import za.sabob.olive.util.OliveUtils;

import java.io.InputStream;

/**
 *
 */
public class OliveTest {

    public void testResourceLoader() {
        Olive olive = new Olive(Mode.DEVELOPMENT);

        String source = OliveUtils.normalize(LoadTest.class, "../loader/test.sql");
        System.out.println(olive.loadContent(source));

        source = OliveUtils.normalize(LoadTest.class, "test.sql");
        System.out.println(source);
        System.out.println(olive.loadContent(OliveUtils.normalize(LoadTest.class, "test.sql")));

        source = OliveUtils.normalize(LoadTest.class, "../loader/test.sql");
        System.out.println(olive.loadContent(source));
    }

    public static void main(String[] args) {
        OliveTest test = new OliveTest();
        test.testResourceLoader();

         InputStream is = OliveUtils.getResourceAsStream(OliveTest.class, "za/sabob/olive/loader/test.sql");
         System.out.println("is:" + is);

         Olive olive = new Olive(Mode.PRODUCTION);
         String source = OliveUtils.normalize(OliveTest.class, "../olive/loader/test.sql");
         //System.out.println(olive.loadFile("../olive/loader/test.sql"));
         System.out.println(olive.loadContent(source));

         //loader.setClassLoader(Pok.class);
         source = OliveUtils.normalize(LoadTest.class, "test.sql");
         //System.out.println(olive.loadFile(LoadTest.class, "test.sql"));
         System.out.println(olive.loadContent(source));

         try {
         Thread.sleep(10000);
         System.out.println(olive.loadContent(OliveUtils.normalize(LoadTest.class, "test.sql")));
         //System.out.println(olive.loadFile(LoadTest.class, "test.sql"));
         } catch (InterruptedException ex) {
         ex.printStackTrace();
         }
    }
}
