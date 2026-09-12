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

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.SentenceLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * The curation scenario with an <b>editable Sentence layer</b>, which is what makes the curation
 * editors take their <em>line</em>-oriented branch
 * ({@code BratLineOrientedAnnotationEditorFactory}) instead of the sentence-oriented one.
 * <p>
 * ⚠️ <b>Two things are required:</b> {@code AnnotationSchemaServiceImpl#isSentenceLayerEditable}
 * demands <em>both</em> the global {@code ui.sentence-layer-editable=true} property <em>and</em> a
 * project Sentence layer that is enabled and not readonly. This initializer can only supply the
 * latter - <b>the property has to be set in {@code settings.properties} as well</b>, or this
 * scenario is indistinguishable from the plain one.
 * <p>
 * ⚠️ Depending on {@link SentenceLayerInitializer} is <b>not sufficient by itself</b>: it creates
 * the layer {@code readonly} and {@code disabled} on purpose, so both flags have to be flipped here
 * afterwards.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#curationSentenceLayerScenarioInitializer}.</b>
 */
@Order(9001)
public class CurationSentenceLayerScenarioInitializer
    extends CurationScenarioInitializer_ImplBase
{
    private final AnnotationSchemaService annotationSchemaService;

    public CurationSentenceLayerScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService,
            AnnotationSchemaService aAnnotationSchemaService)
    {
        super(aDocumentService, aProjectService, aUserService);
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "TEST: curation-ready, editable sentences";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("As the plain curation scenario, but with an enabled, editable Sentence "
                + "layer so the curation editors take their line-oriented branch. Also needs "
                + "ui.sentence-layer-editable=true in the settings.");
    }

    @Override
    protected String getProjectNameSuffix()
    {
        return "curation sentences";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(NamedEntityLayerInitializer.class, SentenceLayerInitializer.class);
    }

    @Override
    protected void configureVariant(Project aProject)
    {
        var sentenceLayer = annotationSchemaService.findLayer(aProject, Sentence.class.getName());
        sentenceLayer.setEnabled(true);
        sentenceLayer.setReadonly(false);
        annotationSchemaService.createOrUpdateLayer(sentenceLayer);
    }
}
