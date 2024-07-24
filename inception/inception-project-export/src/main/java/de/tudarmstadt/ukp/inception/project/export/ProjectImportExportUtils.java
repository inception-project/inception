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
package de.tudarmstadt.ukp.inception.project.export;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ProjectImportExportUtils
{
    public static final String EXPORTED_PROJECT = "exportedproject";

    /**
     * Check if the ZIP file is a project archive.
     * 
     * @param aZipFile
     *            the file.
     * @return if it is valid.
     * @throws ZipException
     *             if the ZIP file is corrupt.
     * @throws IOException
     *             if an I/O error occurs.
     *
     */
    @SuppressWarnings({ "rawtypes" })
    public static boolean isValidProjectArchive(File aZipFile) throws IOException
    {

        boolean isZipValidWebanno = false;
        try (ZipFile zip = new ZipFile(aZipFile)) {
            for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
                if (entry.toString().replace("/", "").startsWith(EXPORTED_PROJECT)
                        && entry.toString().replace("/", "").endsWith(".json")) {
                    isZipValidWebanno = true;
                    break;
                }
            }
        }
        return isZipValidWebanno;
    }

}
