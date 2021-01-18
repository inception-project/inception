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
package de.tudarmstadt.ukp.inception.initializers;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;

@Component
public class BasicSpanLayerInitializer
    implements LayerInitializer
{
    public static final String BASIC_SPAN_LAYER_NAME = "custom.Span";
    public static final String BASIC_SPAN_LABEL_FEATURE_NAME = "label";

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public BasicSpanLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Basic span annotation";
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(BASIC_SPAN_LAYER_NAME, aProject);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class, //
                // Tagsets
                BasicSpanTagSetInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer spanLayer = new AnnotationLayer(BASIC_SPAN_LAYER_NAME, "Span", SPAN_TYPE,
                aProject, false, TOKENS, OVERLAP_ONLY);
        spanLayer.setCrossSentence(false);
        annotationSchemaService.createOrUpdateLayer(spanLayer);

        TagSet spanTagSet = annotationSchemaService
                .getTagSet(BasicSpanTagSetInitializer.BASIC_SPAN_TAG_SET_NAME, aProject);

        annotationSchemaService.createFeature(
                new AnnotationFeature(aProject, spanLayer, BASIC_SPAN_LABEL_FEATURE_NAME, "Label",
                        CAS.TYPE_NAME_STRING, "Span label", spanTagSet));
    }
}
