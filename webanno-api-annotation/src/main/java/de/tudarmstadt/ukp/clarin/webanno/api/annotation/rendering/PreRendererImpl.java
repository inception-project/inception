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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class PreRendererImpl implements PreRenderer
{
    private final AnnotationSchemaService annotationService;
    private final LayerSupportRegistry layerSupportRegistry;

    @Autowired
    public PreRendererImpl(LayerSupportRegistry aLayerSupportRegistry,
            AnnotationSchemaService aAnnotationService)
    {
        layerSupportRegistry = aLayerSupportRegistry;
        annotationService = aAnnotationService;
    }
    
    @Override
    public void render(VDocument aResponse, int windowBeginOffset, int windowEndOffset, JCas aJCas,
            List<AnnotationLayer> aLayers)
    {
        // Render (custom) layers
        for (AnnotationLayer layer : aLayers) {
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            Renderer renderer = layerSupportRegistry.getLayerSupport(layer).getRenderer(layer);
            renderer.render(aJCas, features, aResponse, windowBeginOffset, windowEndOffset);
        }
    }
}
