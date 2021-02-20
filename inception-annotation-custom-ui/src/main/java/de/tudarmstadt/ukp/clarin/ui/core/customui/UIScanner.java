/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
