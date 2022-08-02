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

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link AnnotationAutoConfiguration#preRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_STRUCTURE)
public class PreRendererImpl
    implements PreRenderer
{
    public static final String ID = "PreRenderer";

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
    public String getId()
    {
        return ID;
    }

    @Override
    public void render(VDocument aResponse, RenderRequest aRequest)
    {
        log.trace("Prerenderer.render()");

        CAS cas = aRequest.getCas();
        Validate.notNull(cas, "CAS cannot be null");

        if (aRequest.getVisibleLayers().isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();

        String documentText = cas.getDocumentText();
        int renderBegin = Math.max(0, aRequest.getWindowBeginOffset());
        int renderEnd = Math.min(documentText.length(), aRequest.getWindowEndOffset());
        aResponse.setText(documentText.substring(renderBegin, renderEnd));

        aResponse.setWindowBegin(renderBegin);
        aResponse.setWindowEnd(renderEnd);

        Project project = aRequest.getProject();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        List<AnnotationFeature> supportedFeatures = supportedFeaturesCache.get(project);
        List<AnnotationFeature> allFeatures = allFeaturesCache.get(project);

        // Render (custom) layers
        for (AnnotationLayer layer : aRequest.getVisibleLayers()) {
            List<AnnotationFeature> layerSupportedFeatures = supportedFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());
            List<AnnotationFeature> layerAllFeatures = allFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());
            // We need to pass in *all* the annotation features here because we also to that in
            // other places where we create renderers - and the set of features must always be
            // the same because otherwise the IDs of armed slots would be inconsistent
            LayerSupport<?, ?> layerSupport = layerSupportRegistry.getLayerSupport(layer);
            Renderer renderer = layerSupport.createRenderer(layer, () -> layerAllFeatures);
            renderer.render(cas, layerSupportedFeatures, aResponse, renderBegin, renderEnd);
        }

        if (log.isTraceEnabled()) {
            long duration = currentTimeMillis() - start;
            log.trace(
                    "Prerenderer.render() took {}ms to render {} layers [{}-{}] with {} spans and {} arcs",
                    duration, aRequest.getVisibleLayers().size(), renderBegin, renderEnd,
                    aResponse.spans().size(), aResponse.arcs().size());
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
