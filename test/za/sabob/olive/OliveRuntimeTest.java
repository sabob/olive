/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package za.sabob.olive;

import za.sabob.olive.loader.*;

/**
 *
 */
public class OliveRuntimeTest {

    public static void main(String[] args) {
        ClasspathResourceLoader loader = new ClasspathResourceLoader();

        OliveRuntime runtime = new OliveRuntime(Mode.DEVELOPMENT, loader);
        System.out.println(runtime.loadFile("../loader/test.sql"));

        loader.setClassLoader(LoadTest.class);
        System.out.println(runtime.loadFile("test.sql"));

        try {
            Thread.sleep(5000);
            System.out.println(runtime.loadFile("test.sql"));
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
