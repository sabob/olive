/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.sabob.olive;

import java.io.*;
import za.sabob.olive.loader.*;
import za.sabob.olive.util.*;

/**
 *
 */
public class OliveTest {

    public static void main(String[] args) {

         InputStream is = OliveUtils.getResourceAsStream(OliveTest.class, "za/sabob/olive/loader/test.sql");
         System.out.println("is:" + is);
        
         Olive olive = new Olive(Mode.PRODUCTION);
         String source = OliveUtils.normalize(OliveTest.class, "../olive/loader/test.sql");
         //System.out.println(olive.loadFile("../olive/loader/test.sql"));
         System.out.println(olive.loadFile(source));

         //loader.setClassLoader(Pok.class);
         source = OliveUtils.normalize(LoadTest.class, "test.sql");
         //System.out.println(olive.loadFile(LoadTest.class, "test.sql"));
         System.out.println(olive.loadFile(source));

         try {
         Thread.sleep(10000);
         System.out.println(olive.loadFile(OliveUtils.normalize(LoadTest.class, "test.sql")));
         //System.out.println(olive.loadFile(LoadTest.class, "test.sql"));
         } catch (InterruptedException ex) {
         ex.printStackTrace();
         }
    }
}
