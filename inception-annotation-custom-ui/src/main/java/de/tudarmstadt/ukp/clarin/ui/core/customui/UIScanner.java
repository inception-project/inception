package de.tudarmstadt.ukp.clarin.ui.core.customui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface UIScanner
{
    String SERVICE_NAME = "uiScanner";

    /**
     * Returns the folder which contains the custom UIs
     * 
     * @return the folder with the custom UIs
     * @throws IOException
     */
    public File getCustomUIFolder() throws IOException;

    /**
     * Returns an ArrayList which contains all the names of the files in a folder
     * 
     * @param folder
     * @return an ArrayList with the names of the UIs, which were found
     */
    public ArrayList<String> getUINames(File folder);
}
