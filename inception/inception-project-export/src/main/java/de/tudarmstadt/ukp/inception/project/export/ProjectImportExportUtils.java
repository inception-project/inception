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
    public static boolean isValidProjectArchive(File aZipFile) throws IOException
    {
        try (var zip = new ZipFile(aZipFile)) {
            for (var zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
                var entry = (ZipEntry) zipEnumerate.nextElement();
                var name = entry.getName().replace("/", "");
                if (name.startsWith(EXPORTED_PROJECT) && name.endsWith(".json")) {
                    return true;
                }
            }
        }

        return false;
    }

}
