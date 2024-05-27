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
package de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.config.InceptionSentenceLabelingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionSentenceLabelingProjectInitializersAutoConfiguration#sentenceTagLayerInitializer}.
 * </p>
 */
@Order(30)
public class SentenceLabelLayerInitializer
    implements LayerInitializer
{
    public static final String SENTENCE_LABEL_LAYER_NAME = "custom.SentenceLabel";
    public static final String SENTENCE_LABEL_LABEL_FEATURE_NAME = "label";

    private final AnnotationSchemaService annotationSchemaService;

    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "SentenceLabelLayerInitializer.svg");

    @Autowired
    public SentenceLabelLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Generic sentence classification";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("sentence-labeling-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(SENTENCE_LABEL_LAYER_NAME, aProject);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Tagsets
                SentenceLabelTagSetInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer sentTagLayer = new AnnotationLayer(SENTENCE_LABEL_LAYER_NAME,
                "Sentence Label", SPAN_TYPE, aProject, false, SENTENCES, NO_OVERLAP);
        annotationSchemaService.createOrUpdateLayer(sentTagLayer);

        TagSet sentLabelTagSet = annotationSchemaService
                .getTagSet(SentenceLabelTagSetInitializer.SENTENCE_LABEL_TAG_SET_NAME, aProject);

        annotationSchemaService.createFeature(
                new AnnotationFeature(aProject, sentTagLayer, SENTENCE_LABEL_LABEL_FEATURE_NAME,
                        "Label", CAS.TYPE_NAME_STRING, "Sentence label", sentLabelTagSet));
    }
}
