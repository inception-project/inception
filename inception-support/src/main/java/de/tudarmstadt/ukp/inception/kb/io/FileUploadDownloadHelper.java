/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.kb.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
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

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IFileCleaner fileTracker;

    private final String INCEPTION_TMP_FILE_PREFIX = "inception_file";

    public FileUploadDownloadHelper(Application application)
    {
        fileTracker = application.getResourceSettings().getFileCleaner();
    }

    /**
     * Writes the input stream to a temporary file. The file is deleted if the object behind marker
     * is garbage collected. The temporary file will keep its extension based on the specified file
     * name.
     *
     * @param fileUpload The file upload handle to write to the temporary file
     * @param marker The object to whose lifetime the temporary file is bound
     * @return A handle to the created temporary file
     */
    public File writeFileUploadToTemporaryFile(FileUpload fileUpload, Object marker) throws IOException
    {
        String fileName = fileUpload.getClientFileName();
        File tmpFile = File.createTempFile(INCEPTION_TMP_FILE_PREFIX, fileName);
        log.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        fileTracker.track(tmpFile, marker);
        try (InputStream is = fileUpload.getInputStream()) {
            FileUtils.copyInputStreamToFile(is, tmpFile);
        }
        return tmpFile;
    }

    public File writeFileDownloadToTemporaryFile(String downloadUrl, Object marker) throws
        IOException
    {
        Path pathName = Paths.get(downloadUrl);
        String fileName = pathName.getFileName().toString();
        File tmpFile = File.createTempFile(INCEPTION_TMP_FILE_PREFIX, fileName);
        log.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        fileTracker.track(tmpFile, marker);
        FileUtils.copyURLToFile(new URL(downloadUrl), tmpFile);
        return tmpFile;
    }

    public File writeClasspathResourceToTemporaryFile(String aLocation, Object marker) throws
        IOException
    {
        String fileName = getNameFromClassPathResource(aLocation);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        File tmpFile = File
            .createTempFile(INCEPTION_TMP_FILE_PREFIX, fileName);
        fileTracker.track(tmpFile, marker);
        log.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        try (InputStream is = resolver.getResource(aLocation).getInputStream();
            FileOutputStream os = new FileOutputStream(tmpFile)) {
            IOUtils.copy(is, os);
        }
        return tmpFile;
    }

    private String getNameFromClassPathResource(String aResource) {
        return aResource.substring(aResource.lastIndexOf("/") + 1 );
    }
}
