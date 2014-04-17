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
public class OliveTest {

    public static void main(String[] args) {
        Olive olive = new Olive(Mode.DEVELOPMENT);
        System.out.println(olive.loadFile("../loader/test.sql"));

        //loader.setClassLoader(Pok.class);
        System.out.println(olive.loadFile("test.sql", LoadTest.class));

        try {
            Thread.sleep(0);
            olive.setClassLoader(LoadTest.class);
            System.out.println(olive.loadFile("test.sql"));
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

    }
}
