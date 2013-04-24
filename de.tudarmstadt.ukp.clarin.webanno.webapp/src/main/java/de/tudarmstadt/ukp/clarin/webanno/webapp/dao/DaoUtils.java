/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.clarin.webanno.webapp.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import de.tudarmstadt.ukp.dkpro.core.io.bincas.SerializedCasWriter;

/**
 * a Util class for {@link RepositoryServiceDbData} and {@link AnnotationServiceImpl} classes
 * @author Seid Muhie Yimam
 *
 */
public class DaoUtils
{   /**
    * While exporting annotation documents, some of the writers generate multiple outputs, such as the
    * the {@link SerializedCasWriter}. This method generates a zip file if the exported file do
    * contain multiple file output
    */
    public static void zipFolder(File srcFolder, File destZipFile)
        throws Exception
    {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip(new File(""), srcFolder, zip);
        zip.flush();
        zip.close();
    }

    private static void addFileToZip(File path, File srcFile, ZipOutputStream zip)
        throws Exception
    {

        if (srcFile.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        }
        else {
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + srcFile.getName()));
            IOUtils.copy(in, zip);
        }
    }

    private static void addFolderToZip(File path, File srcFolder, ZipOutputStream zip)
        throws Exception
    {
        // File folder = new File(srcFolder);

        for (String fileName : srcFolder.list()) {
            if (path.equals("")) {
                addFileToZip(srcFolder, new File(srcFolder + "/" + fileName), zip);
            }
            else {
                addFileToZip(new File(path + "/" + srcFolder.getName()), new File(srcFolder + "/"
                        + fileName), zip);
            }
        }
    }
}
