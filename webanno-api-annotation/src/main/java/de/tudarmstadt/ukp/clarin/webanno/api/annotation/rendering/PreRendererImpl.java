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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import java.util.List;

import org.apache.uima.jcas.JCas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class PreRendererImpl implements PreRenderer
{
    private FeatureSupportRegistry featureSupportRegistry;
    private AnnotationSchemaService annotationService;

    @Autowired
    public PreRendererImpl(FeatureSupportRegistry aFeatureSupportRegistry,
            AnnotationSchemaService aAnnotationService)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        annotationService = aAnnotationService;
    }
    
    @Override
    public void render(VDocument aResponse, AnnotatorState aState, JCas aJCas,
            List<AnnotationLayer> aLayers)
    {
        // Render (custom) layers
        for (AnnotationLayer layer : aLayers) {
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            TypeAdapter adapter = annotationService.getAdapter(layer);
            Renderer renderer = getRenderer(adapter);
            renderer.render(aJCas, features, aResponse, aState);
        }
    }

    private Renderer getRenderer(TypeAdapter aTypeAdapter)
    {
        if (aTypeAdapter instanceof SpanAdapter) {
            return new SpanRenderer((SpanAdapter) aTypeAdapter, featureSupportRegistry);
        }
        else if (aTypeAdapter instanceof ArcAdapter) {
            return new RelationRenderer((ArcAdapter) aTypeAdapter, featureSupportRegistry);
        }
        else if (aTypeAdapter instanceof ChainAdapter) {
            return new ChainRenderer((ChainAdapter) aTypeAdapter, featureSupportRegistry);
        }
        else {
            throw new IllegalArgumentException(
                    "Unknown adapter type [" + aTypeAdapter.getClass().getName() + "]");
        }
    }
}
