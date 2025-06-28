/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.io.FileUtils;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactoryImplBase;
import de.tudarmstadt.ukp.inception.search.index.mtas.config.MtasDocumentIndexAutoConfiguration;

/**
 * Support for MTAS-based internal search.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link MtasDocumentIndexAutoConfiguration#mtasDocumentIndexFactory}.
 * </p>
 */
public class MtasDocumentIndexFactory
    extends PhysicalIndexFactoryImplBase
{
    public static final String INDEX = "indexMtas";

    private final DocumentService documentService;
    private final RepositoryProperties repositoryProperties;
    private final FeatureIndexingSupportRegistry featureIndexingSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;

    public MtasDocumentIndexFactory(DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
        featureIndexingSupportRegistry = aFeatureIndexingSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public String getDisplayName()
    {
        return "mtasDocumentIndexFactory";
    }

    @Override
    public File getIndexDir(Project aProject)
    {
        return repositoryProperties.getPath().toPath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(aProject.getId())) //
                .resolve(INDEX) //
                .toFile();
    }

    @Override
    public long getIndexSize(Project aProject) throws IOException
    {
        try {
            return FileUtils.sizeOfDirectory(getIndexDir(aProject));
        }
        catch (UncheckedIOException e) {
            throw new IOException(e);
        }
    }

    @Override
    public PhysicalIndex getPhysicalIndex(Project aProject)
    {
        return new MtasDocumentIndex(aProject, documentService, getIndexDir(aProject),
                featureIndexingSupportRegistry, featureSupportRegistry);
    }
}
