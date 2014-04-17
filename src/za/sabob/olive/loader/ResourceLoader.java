/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package za.sabob.olive.loader;

import java.io.*;

/**
 *
 */
public interface ResourceLoader {

    public InputStream getResourceStream(String source);
}
