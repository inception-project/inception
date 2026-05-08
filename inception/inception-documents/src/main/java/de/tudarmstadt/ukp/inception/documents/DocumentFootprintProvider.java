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
package de.tudarmstadt.ukp.inception.documents;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.SOURCE_FOLDER;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.footprint.Footprint;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProvider;

public class DocumentFootprintProvider
    implements FootprintProvider
{
    private final DocumentStorageServiceImpl documentStorageService;

    public DocumentFootprintProvider(DocumentStorageServiceImpl aDocumentStorageService)
    {
        documentStorageService = aDocumentStorageService;
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Override
    public List<Footprint> getFootprint(Project aProject)
    {
        try {
            var sourceDocumentsSize = 0L;
            var annotationsSize = 0L;
            var annotationBackupSize = 0L;
            var documentsPath = documentStorageService.getDocumentsFolder(aProject);
            try (var docFolders = newDirectoryStream(documentsPath)) {
                for (var docFolder : docFolders) {
                    if (!isDirectory(docFolder)) {
                        continue;
                    }

                    try {
                        sourceDocumentsSize += sizeOfDirectory(
                                docFolder.resolve(SOURCE_FOLDER).toFile());
                    }
                    catch (UncheckedIOException e) {
                        // Ignore
                    }

                    try {
                        try (var files = newDirectoryStream(docFolder.resolve(ANNOTATION_FOLDER))) {
                            for (var file : files) {
                                if (!isRegularFile(file)) {
                                    continue;
                                }
                                String fileName = file.getFileName().toString();
                                long size = Files.size(file);

                                if (fileName.endsWith(".ser")) {
                                    annotationsSize += size;
                                }
                                else if (fileName.endsWith(".bak")) {
                                    annotationBackupSize += size;
                                }
                            }
                        }
                    }
                    catch (UncheckedIOException e) {
                        // Ignore
                    }
                }
            }

            return asList( //
                    new Footprint("Documents", sourceDocumentsSize, "lightgreen"),
                    new Footprint("Annotations", annotationsSize, "lightcoral"),
                    new Footprint("Annotation backups", annotationBackupSize, "palevioletred"));
        }
        catch (IOException e) {
            return emptyList();
        }
    }
}
