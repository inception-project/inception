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
package de.tudarmstadt.ukp.clarin.webanno.brat.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.ZipUtils;

public class DaoUtilsTest
{
    private Log LOG = LogFactory.getLog(getClass());
    @SuppressWarnings("resource")
    @Test
    public void testZipFolder()
    {

        File toBeZippedFiles = new File("src/test/resources");
        File zipedFiles = new File("target/"
                + toBeZippedFiles.getName() + ".zip");
        try {
            ZipUtils.zipFolder(toBeZippedFiles, zipedFiles);
        }
        catch (Exception e) {
            LOG.info("Zipping fails" + ExceptionUtils.getRootCauseMessage(e));
        }

        FileInputStream fs;
        try {
            fs = new FileInputStream(zipedFiles);
            ZipInputStream zis = new ZipInputStream(fs);
            ZipEntry zE;
            while ((zE = zis.getNextEntry()) != null) {
                for (File toBeZippedFile : toBeZippedFiles.listFiles()) {
                    if ((FilenameUtils.getBaseName(toBeZippedFile.getName())).equals(FilenameUtils
                            .getBaseName(zE.getName()))) {
                        // Extract the zip file, save it to temp and compare
                        ZipFile zipFile = new ZipFile(zipedFiles);
                        ZipEntry zipEntry = zipFile.getEntry(zE.getName());
                        InputStream is = zipFile.getInputStream(zipEntry);
                        File temp = File.createTempFile("temp", ".txt");
                        OutputStream out = new FileOutputStream(temp);
                        IOUtils.copy(is, out);
                        FileUtils.contentEquals(toBeZippedFile, temp);
                    }
                }
                zis.closeEntry();
            }
        }
        catch (FileNotFoundException e) {
            LOG.info("zipped file not found " + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            LOG.info("Unable to get file " + ExceptionUtils.getRootCauseMessage(e));
        }

    }

}
