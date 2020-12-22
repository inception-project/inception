/*
 * Copyright 2017
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
 */
package de.tudarmstadt.ukp.inception.search.index.mtas;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
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
    private final AnnotationSchemaService schemaService;
    private final DocumentService documentService;
    private final RepositoryProperties repositoryProperties;
    private final FeatureIndexingSupportRegistry featureIndexingSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;

    @Autowired
    public MtasDocumentIndexFactory(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService, RepositoryProperties aRepositoryProperties,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        schemaService = aSchemaService;
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
    public PhysicalIndex getPhysicalIndex(Project aProject)
    {
        MtasDocumentIndex openIndex = MtasDocumentIndex.getIndex(aProject.getId());
        if (openIndex != null) {
            throw new IllegalStateException(
                    "Trying to create new index for project already having an open index!");
        }

        return new MtasDocumentIndex(aProject, documentService, schemaService,
                repositoryProperties.getPath().getAbsolutePath(), featureIndexingSupportRegistry,
                featureSupportRegistry);
    }
}
