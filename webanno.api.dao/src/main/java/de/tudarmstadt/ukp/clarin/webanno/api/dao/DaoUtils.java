/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * A utility class for {@link RepositoryServiceDbData} and {@link AnnotationServiceImpl} classes
 * 
 * @author Seid Muhie Yimam
 */
public class DaoUtils
{
    private static String srcPath;
    
    /**
     * While exporting annotation documents, some of the writers generate multiple outputs, e.g. a
     * type system file in addition to the annotation data. This method generates a zip file if the
     * exported file do contain multiple file output
     */
    public static void zipFolder(File srcFolder, File destZipFile)
        throws Exception
    {
       srcPath = srcFolder.getName();
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
           /* // We don't need the folder name inside the zi[p
            if(path.getName().equals(srcPath)){
                File subFolder = new File(srcFile.getAbsolutePath().replace(srcPath, ""));
                addFolderToZip(path, subFolder, zip);
            }
            else {
                addFolderToZip(path, srcFile, zip);
            }*/
        }
        else {
            FileInputStream in = new FileInputStream(srcFile);
            if(path.getName().equals(srcPath)){
                zip.putNextEntry(new ZipEntry("/" + srcFile.getName()));
            }
            else {
                zip.putNextEntry(new ZipEntry(path + "/" + srcFile.getName()));
            }
            IOUtils.copy(in, zip);
        }
    }

    private static void addFolderToZip(File path, File srcFolder, ZipOutputStream zip)
        throws Exception
    {

        for (String fileName : srcFolder.list()) {
            if (path.equals("")) {
                addFileToZip(srcFolder, new File(srcFolder + "/" + fileName), zip);
            }
            else {
                if(path.getPath()!=null && path.getName().equals(srcPath)) {
                    path = new File("");
                }
                addFileToZip(new File(path + "/" + srcFolder.getName()), new File(srcFolder + "/"
                        + fileName), zip);
            }
        }
    }
}
