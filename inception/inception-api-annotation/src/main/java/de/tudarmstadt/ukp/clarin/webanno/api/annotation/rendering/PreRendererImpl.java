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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class PreRendererImpl
    implements PreRenderer
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AnnotationSchemaService annotationService;
    private final LayerSupportRegistry layerSupportRegistry;

    private LoadingCache<Project, List<AnnotationFeature>> supportedFeaturesCache;
    private LoadingCache<Project, List<AnnotationFeature>> allFeaturesCache;

    @Autowired
    public PreRendererImpl(LayerSupportRegistry aLayerSupportRegistry,
            AnnotationSchemaService aAnnotationService)
    {
        layerSupportRegistry = aLayerSupportRegistry;
        annotationService = aAnnotationService;

        supportedFeaturesCache = Caffeine.newBuilder() //
                .expireAfterAccess(5, MINUTES) //
                .maximumSize(10 * 1024) //
                .build(annotationService::listSupportedFeatures);
        allFeaturesCache = Caffeine.newBuilder() //
                .expireAfterAccess(5, MINUTES) //
                .maximumSize(10 * 1024) //
                .build(annotationService::listAnnotationFeature);
    }

    @Override
    public void render(VDocument aResponse, int windowBegin, int windowEnd, CAS aCas,
            List<AnnotationLayer> aLayers)
    {
        log.trace("render()");

        Validate.notNull(aCas, "CAS cannot be null");

        if (aLayers.isEmpty()) {
            return;
        }

        // The project for all layers must be the same, so we just fetch the project from the
        // first layer
        Project project = aLayers.get(0).getProject();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        List<AnnotationFeature> supportedFeatures = supportedFeaturesCache.get(project);
        List<AnnotationFeature> allFeatures = allFeaturesCache.get(project);

        // Create the renderers
        List<Renderer> renderers = new ArrayList<>();
        for (AnnotationLayer layer : aLayers) {
            List<AnnotationFeature> layerAllFeatures = allFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());
            // We need to pass in *all* the annotation features here because we also to that in
            // other places where we create renderers - and the set of features must always be
            // the same because otherwise the IDs of armed slots would be inconsistent
            renderers.add(layerSupportRegistry.getLayerSupport(layer) //
                    .createRenderer(layer, () -> layerAllFeatures));
        }

        // Bring the renderers into an order per @Order annotation on the renderer classes.
        // The idea is in particular that spans are rendered before the relations which connect to
        // the spans.
        AnnotationAwareOrderComparator.sort(renderers);

        // Render (custom) layers
        for (Renderer renderer : renderers) {
            List<AnnotationFeature> layerSupportedFeatures = supportedFeatures.stream() //
                    .filter(feature -> feature.getLayer()
                            .equals(renderer.getTypeAdapter().getLayer())) //
                    .collect(toList());
            renderer.render(aCas, layerSupportedFeatures, aResponse, windowBegin, windowEnd);
        }
    }

    @EventListener
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        supportedFeaturesCache.asMap().keySet()
                .removeIf(key -> Objects.equals(key.getId(), aEvent.getProject().getId()));
        allFeaturesCache.asMap().keySet()
                .removeIf(key -> Objects.equals(key.getId(), aEvent.getProject().getId()));
    }
}
