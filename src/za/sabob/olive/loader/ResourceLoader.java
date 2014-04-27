/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package za.sabob.olive.loader;

import java.io.*;

/**
 * Provides an interface for loading resources such as SQL files.
 */
public interface ResourceLoader {

    /**
     * Returns the InputStream of the resource for the given source name.
     *
     * @param source the name of the resource to load.
     * @return the InputStream for the given source name
     */
    public InputStream getResourceStream(String source);
}
