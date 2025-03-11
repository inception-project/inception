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
package de.tudarmstadt.ukp.inception.support.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.file.IFileCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class FileUploadDownloadHelper
{
    public static final String INCEPTION_TMP_FILE_PREFIX = "inception_file";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IFileCleaner fileTracker;

    public FileUploadDownloadHelper(Application application)
    {
        fileTracker = application.getResourceSettings().getFileCleaner();
    }

    /**
     * Writes the input stream to a temporary file. The file is deleted if the object behind marker
     * is garbage collected. The temporary file will keep its extension based on the specified file
     * name.
     *
     * @param fileUpload
     *            The file upload handle to write to the temporary file
     * @param marker
     *            The object to whose lifetime the temporary file is bound
     * @return A handle to the created temporary file
     * @throws IOException
     *             if there was an I/O problem writing to the file
     */
    public File writeFileUploadToTemporaryFile(FileUpload fileUpload, Object marker)
        throws IOException
    {
        var fileName = fileUpload.getClientFileName();
        var tmpFile = File.createTempFile(INCEPTION_TMP_FILE_PREFIX, fileName);
        LOG.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        fileTracker.track(tmpFile, marker);
        try (var is = fileUpload.getInputStream()) {
            FileUtils.copyInputStreamToFile(is, tmpFile);
        }
        return tmpFile;
    }

    public File writeFileDownloadToTemporaryFile(String downloadUrl, Object marker)
        throws IOException
    {
        var pathName = Paths.get(downloadUrl);
        var fileName = pathName.getFileName().toString();
        var tmpFile = File.createTempFile(INCEPTION_TMP_FILE_PREFIX, fileName);
        LOG.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        fileTracker.track(tmpFile, marker);
        FileUtils.copyURLToFile(new URL(downloadUrl), tmpFile);
        return tmpFile;
    }

    public File writeClasspathResourceToTemporaryFile(String aLocation, Object marker)
        throws IOException
    {
        var fileName = getNameFromClassPathResource(aLocation);
        var resolver = new PathMatchingResourcePatternResolver();
        var tmpFile = File.createTempFile(INCEPTION_TMP_FILE_PREFIX, fileName);
        fileTracker.track(tmpFile, marker);
        LOG.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        try (var is = resolver.getResource(aLocation).getInputStream();
                FileOutputStream os = new FileOutputStream(tmpFile)) {
            IOUtils.copy(is, os);
        }
        return tmpFile;
    }

    private String getNameFromClassPathResource(String aResource)
    {
        return aResource.substring(aResource.lastIndexOf("/") + 1);
    }
}
