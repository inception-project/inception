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
package de.tudarmstadt.ukp.inception.diam.editor.lazydetails;

import static de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler.PARAM_LAZY_DETAIL_DATABASE;
import static de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler.PARAM_LAZY_DETAIL_KEY;
import static de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler.PARAM_TYPE;
import static de.tudarmstadt.ukp.inception.diam.editor.actions.LazyDetailsHandler.COMMAND;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.string.StringValue;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailResult;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#lazyDetailsLookupService}.
 * </p>
 */
public class LazyDetailsLookupServiceImpl
    implements LazyDetailsLookupService
{
    private final AnnotationSchemaService annotationService;
    private final AnnotationEditorExtensionRegistry extensionRegistry;
    private final LayerSupportRegistry layerSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;

    public LazyDetailsLookupServiceImpl(AnnotationSchemaService aAnnotationService,
            AnnotationEditorExtensionRegistry aExtensionRegistry,
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        annotationService = aAnnotationService;
        extensionRegistry = aExtensionRegistry;
        layerSupportRegistry = aLayerSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public LazyDetailsResponse lookupLazyDetails(IRequestParameters request, VID paramId,
            CasProvider aCas, SourceDocument aDocument, User aUser, int windowBeginOffset,
            int windowEndOffset)
        throws AnnotationException, IOException
    {
        LazyDetailsResponse response = new LazyDetailsResponse(COMMAND);

        var topicParam = request.getParameterValue(PARAM_LAZY_DETAIL_DATABASE);
        var keyParam = request.getParameterValue(PARAM_LAZY_DETAIL_KEY);
        var layerParam = request.getParameterValue(PARAM_TYPE);

        if (layerParam.isEmpty() || topicParam.isEmpty()) {
            return response;
        }

        Project project = aDocument.getProject();
        String topic = topicParam.toString();
        long layerId = layerParam.toLong();
        AnnotationLayer layer = annotationService.getLayer(project, layerId)
                .orElseThrow(() -> new AnnotationException(
                        "Layer with ID [" + layerId + "] does not exist in project " + project));

        List<VLazyDetailResult> details = new ArrayList<>();

        renderExtensionLevelDetail(paramId, aDocument, aUser, keyParam, topic, layer, details);

        // Is it a layer-level lazy detail?
        if (Renderer.QUERY_LAYER_LEVEL_DETAILS.equals(topic)) {
            renderLayerLevelDetail(paramId, aCas, windowBeginOffset, windowEndOffset, layer,
                    details);
        }
        // Is it a feature-level lazy detail?
        // We interpret the key as the feature value or as a kind of query to be handled by the
        // feature support
        else if (!keyParam.isEmpty()) {
            renderFeatureLevelDetail(paramId, aCas, keyParam, topic, layer, details);
        }

        response.setResults(details.stream() //
                .map(d -> new LazyDetailQuery(d.getLabel(), d.getValue())) //
                .collect(toList()));

        return response;
    }

    private void renderFeatureLevelDetail(VID paramId, CasProvider aCas, StringValue keyParam,
            String topic, AnnotationLayer layer, List<VLazyDetailResult> details)
        throws IOException
    {
        AnnotationFeature feature = annotationService.getFeature(topic, layer);
        featureSupportRegistry.findExtension(feature).orElseThrow()
                .renderLazyDetails(aCas.get(), feature, paramId, keyParam.toString()) //
                .forEach(details::add);
    }

    private void renderLayerLevelDetail(VID paramId, CasProvider aCas, int windowBeginOffset,
            int windowEndOffset, AnnotationLayer layer, List<VLazyDetailResult> details)
        throws IOException
    {
        layerSupportRegistry.getLayerSupport(layer)
                .createRenderer(layer, () -> annotationService.listAnnotationFeature(layer))
                .renderLazyDetails(aCas.get(), paramId, windowBeginOffset, windowEndOffset)
                .forEach(details::add);
    }

    private void renderExtensionLevelDetail(VID paramId, SourceDocument aSourceDocument, User aUser,
            StringValue keyParam, String topic, AnnotationLayer layer,
            List<VLazyDetailResult> details)
    {
        // Only applies to synthetic annotations (i.e. from extensions)
        if (!paramId.isSynthetic()) {
            return;
        }

        AnnotationFeature feature = annotationService.getFeature(topic, layer);

        String extensionId = paramId.getExtensionId();
        extensionRegistry.getExtension(extensionId)
                .renderLazyDetails(aSourceDocument, aUser, paramId, feature,
                        keyParam.toOptionalString()) //
                .forEach(details::add);
    }
}
