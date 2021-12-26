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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.request.cycle.RequestCycle;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationAutoConfiguration#renderingPipeline}.
 * </p>
 */
public class RenderingPipelineImpl
    implements RenderingPipeline
{
    private final PreRenderer preRenderer;
    private final AnnotationEditorExtensionRegistry extensionRegistry;
    private final AnnotationSchemaService annotationService;
    private final ColoringService coloringService;
    private final AnnotationEditorProperties properties;

    public RenderingPipelineImpl(PreRenderer aPreRenderer,
            AnnotationEditorExtensionRegistry aExtensionRegistry,
            AnnotationSchemaService aAnnotationService, ColoringService aColoringService,
            AnnotationEditorProperties aProperties)
    {
        preRenderer = aPreRenderer;
        extensionRegistry = aExtensionRegistry;
        annotationService = aAnnotationService;
        coloringService = aColoringService;
        properties = aProperties;
    }

    @Override
    public VDocument render(RenderRequest aRequest)
    {
        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, aRequest, getLayersToRender(aRequest.getState()));

        extensionRegistry.fireRender(vdoc, aRequest);

        maybeNotifyWicketComponentsOnPage(vdoc, aRequest);

        if (aRequest.getState().getSelection().getAnnotation().isSet()) {
            vdoc.add(new VAnnotationMarker(VMarker.FOCUS,
                    aRequest.getState().getSelection().getAnnotation()));
        }

        new LabelRenderer().render(vdoc, aRequest);

        ColorRenderer colorRenderer = new ColorRenderer(annotationService, coloringService);
        colorRenderer.render(vdoc, aRequest);

        return vdoc;
    }

    private void maybeNotifyWicketComponentsOnPage(VDocument aVDoc, RenderRequest aRequest)
    {
        // Fire render event into UI
        RequestCycle.get().find(IPageRequestHandler.class).ifPresent(handler -> {
            Page page = (Page) handler.getPage();
            page.send(page, Broadcast.BREADTH,
                    new RenderAnnotationsEvent(
                            RequestCycle.get().find(IPartialPageRequestHandler.class).get(),
                            aRequest.getCas(), aRequest.getState(), aVDoc));
        });
    }

    private List<AnnotationLayer> getLayersToRender(AnnotatorState state)
    {
        List<AnnotationLayer> layersToRender = new ArrayList<>();
        for (AnnotationLayer layer : state.getAnnotationLayers()) {
            if (!layer.isEnabled()) {
                continue;
            }

            if (!properties.isTokenLayerEditable()
                    && Token.class.getName().equals(layer.getName())) {
                continue;
            }

            if (!properties.isSentenceLayerEditable()
                    && Sentence.class.getName().equals(layer.getName())) {
                continue;
            }

            if (layer.getType().equals(CHAIN_TYPE) && CURATION == state.getMode()) {
                continue;
            }

            layersToRender.add(layer);
        }
        return layersToRender;
    }
}
