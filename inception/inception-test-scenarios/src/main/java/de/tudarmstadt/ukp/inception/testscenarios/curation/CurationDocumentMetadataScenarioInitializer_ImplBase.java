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

import static de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport.FEATURE_NAME_ORDER;
import static de.tudarmstadt.ukp.inception.project.initializers.doclabeling.BasicDocumentLabelLayerInitializer.BASIC_DOCUMENT_LABEL_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.doclabeling.BasicDocumentLabelLayerInitializer.BASIC_DOCUMENT_LABEL_LAYER_NAME;
import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.initializers.doclabeling.BasicDocumentLabelLayerInitializer;
import de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccountService;

/**
 * The curation-ready project with two documents, plus a singleton "Document Label" layer with a
 * closed tagset on which each annotator sets the label given by {@link #getLabel(int)}.
 */
public abstract class CurationDocumentMetadataScenarioInitializer_ImplBase
    extends CurationScenarioInitializer_ImplBase
{
    public CurationDocumentMetadataScenarioInitializer_ImplBase(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService,
            ScenarioAccountService aScenarioAccountService)
    {
        super(aDocumentService, aProjectService, aUserService, aScenarioAccountService);
    }

    /**
     * Two documents, so that a choice made while curating one document - such as the annotator
     * selection in the curation sidebar - can be checked against the first opening of the other.
     */
    @Override
    protected List<String> listDocuments()
    {
        return asList("curation-sample.txt", "curation-sample-editable.txt");
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(NamedEntityLayerInitializer.class, BasicDocumentLabelLayerInitializer.class);
    }

    /**
     * @return the document label the annotator with the given number chooses.
     */
    protected abstract String getLabel(int aVariant);

    @Override
    protected void annotateVariant(CAS aCas, int aVariant)
    {
        var type = aCas.getTypeSystem().getType(BASIC_DOCUMENT_LABEL_LAYER_NAME);
        var label = aCas.createFS(type);
        label.setStringValue(type.getFeatureByBaseName(BASIC_DOCUMENT_LABEL_LABEL_FEATURE_NAME),
                getLabel(aVariant));
        // The only label in the CAS - the editor numbers labels from 1 when it creates them
        label.setIntValue(type.getFeatureByBaseName(FEATURE_NAME_ORDER), 1);
        aCas.addFsToIndexes(label);
    }
}
