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

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.SOURCE_FOLDER;
import static java.nio.file.Files.createDirectories;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;

public class DocumentStorageServiceImpl
    implements DocumentStorageService
{
    private final RepositoryProperties repositoryProperties;

    public DocumentStorageServiceImpl(RepositoryProperties aRepositoryProperties)
    {
        repositoryProperties = aRepositoryProperties;
    }

    Path getSourceDocumentFolder(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");
        Validate.notNull(aDocument.getProject().getId(),
                "Source document's project must have an ID");
        Validate.notNull(aDocument.getId(), "Source document must have an ID");

        return getDocumentsFolder(aDocument.getProject()) //
                .resolve(Long.toString(aDocument.getId())) //
                .resolve(SOURCE_FOLDER);
    }

    Path getDocumentsFolder(Project aProject)
    {
        return repositoryProperties.getPath().toPath() //
                .toAbsolutePath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(aProject.getId())) //
                .resolve(DOCUMENT_FOLDER)//
        ;
    }

    @Override
    public boolean existsSourceDocumentFile(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        var path = getSourceDocumentFile(aDocument).toPath();
        return Files.exists(path);
    }

    @Override
    public void renameSourceDocumentFile(SourceDocument aDocument, String aNewName)
        throws IOException
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");
        Validate.notBlank(aNewName, "Parameter [newName] must be specified");

        var oldFile = getSourceDocumentFile(aDocument);
        if (!oldFile.exists()) {
            throw new IllegalStateException("Source document file does not exist: " + oldFile);
        }

        var newFile = getSourceDocumentFolder(aDocument).resolve(aNewName).toFile();
        if (newFile.exists()) {
            throw new IllegalStateException("Target file already exists: " + newFile);
        }

        try {
            Files.move(oldFile.toPath(), newFile.toPath());
        }
        catch (IOException e) {
            throw new IOException("Failed to rename source document file from [" + oldFile.getName()
                    + "] to [" + newFile.getName() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeSourceDocumentFile(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        var path = getSourceDocumentFolder(aDocument);
        if (Files.exists(path)) {
            FileUtils.forceDelete(path.toFile());
        }
    }

    @Override
    public File getSourceDocumentFile(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        return getSourceDocumentFolder(aDocument).resolve(aDocument.getName()).toFile();
    }

    @Override
    public IResourceStream getSourceDocumentResourceStream(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        return getSourceDocumentResourceStream(aDocument, null);
    }

    @Override
    public IResourceStream getSourceDocumentResourceStream(SourceDocument aDocument,
            String aContentType)
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        var file = getSourceDocumentFile(aDocument);

        return new FileResourceStream(file)
        {
            private static final long serialVersionUID = 5985138568430773008L;

            @Override
            public String getContentType()
            {
                if (aContentType != null) {
                    return aContentType;
                }

                return super.getContentType();
            }
        };
    }

    @Override
    public InputStream openSourceDocumentFile(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return new FileInputStream(getSourceDocumentFile(aDocument));
    }

    @Override
    public void writeSourceDocumentFile(SourceDocument aDocument, InputStream aIs)
        throws IOException
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");
        Validate.notNull(aIs, "Parameter [inputStream] must be specified");

        var targetFile = getSourceDocumentFile(aDocument);
        createDirectories(targetFile.getParentFile().toPath());

        try (var os = new FileOutputStream(targetFile)) {
            copyLarge(aIs, os);
        }
    }

    @Override
    public void copySourceDocumentFile(SourceDocument aDocument, File aTargetFile)
        throws IOException
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        var documentFile = getSourceDocumentFile(aDocument);
        FileUtils.copyFile(documentFile, aTargetFile);
    }

    @Override
    public long getSourceDocumentFileSize(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Parameter [sourceDocument] must be specified");

        return FileUtils.sizeOf(getSourceDocumentFile(aDocument));
    }
}
