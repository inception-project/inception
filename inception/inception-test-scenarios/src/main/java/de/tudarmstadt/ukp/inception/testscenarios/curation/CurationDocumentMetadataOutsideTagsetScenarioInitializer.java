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
package de.tudarmstadt.ukp.inception.testscenarios.curation;

import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccountService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * Like {@link CurationDocumentMetadataScenarioInitializer}, but the second annotator's document
 * label is not part of the closed tagset of the "Document Label" layer.
 * <p>
 * This exists to test that merging such a label in the document metadata sidebar is rejected and
 * leaves the curation document unchanged.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#curationDocumentMetadataOutsideTagsetScenarioInitializer}.</b>
 */
@Order(9000)
public class CurationDocumentMetadataOutsideTagsetScenarioInitializer
    extends CurationDocumentMetadataScenarioInitializer_ImplBase
{
    private static final String LABEL_OUTSIDE_TAGSET = "Unlisted label";

    public CurationDocumentMetadataOutsideTagsetScenarioInitializer(
            DocumentService aDocumentService, ProjectService aProjectService, UserDao aUserService,
            ScenarioAccountService aScenarioAccountService)
    {
        super(aDocumentService, aProjectService, aUserService, aScenarioAccountService);
    }

    @Override
    public String getName()
    {
        return "TEST: curation-ready, document metadata outside the tagset";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Two curatable documents where one annotator's document label is not "
                + "in the closed tagset. For testing that merging it is rejected.");
    }

    @Override
    protected String getProjectNameSuffix()
    {
        return "curation document metadata outside tagset";
    }

    @Override
    protected Optional<String> describeVariant()
    {
        return Optional.of("""
                Both documents also carry a **Document Label**: **scenario-annotator-1** chose \
                `Label 1`, **scenario-annotator-2** `%s`, which is not in the closed tagset \
                of the layer.""".formatted(LABEL_OUTSIDE_TAGSET));
    }

    @Override
    protected String getLabel(int aVariant)
    {
        return aVariant == 2 ? LABEL_OUTSIDE_TAGSET : "Label " + aVariant;
    }
}
