/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.sabob.olive;

import za.sabob.olive.loader.*;
import za.sabob.olive.util.*;

/**
 *
 */
public class OliveRuntimeTest {

    public static void main(String[] args) {
        ClasspathResourceLoader loader = new ClasspathResourceLoader();

        OliveRuntime runtime = new OliveRuntime(Mode.DEVELOPMENT, loader);
        String source = OliveUtils.normalize(LoadTest.class, "../loader/test.sql");
        System.out.println(runtime.loadFile(source));

        //loader.setClassLoader(LoadTest.class);
        source = OliveUtils.normalize(LoadTest.class, "test.sql");
        System.out.println(source);
        System.out.println(runtime.loadFile(OliveUtils.normalize(LoadTest.class, "test.sql")));

        source = OliveUtils.normalize(LoadTest.class, "../loader/test.sql");
        System.out.println(runtime.loadFile(source));

    }
}
