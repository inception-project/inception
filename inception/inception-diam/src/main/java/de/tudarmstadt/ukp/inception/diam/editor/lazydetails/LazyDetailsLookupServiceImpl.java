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

import static de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler.PARAM_TYPE;
import static de.tudarmstadt.ukp.inception.diam.editor.actions.LazyDetailsHandler.COMMAND;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
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
    public LazyDetailsResponse lookupLazyDetails(IRequestParameters request, VID aVid,
            CasProvider aCas, SourceDocument aDocument, User aUser, int windowBeginOffset,
            int windowEndOffset)
        throws AnnotationException, IOException
    {
        var response = new LazyDetailsResponse(COMMAND);

        var layerParam = request.getParameterValue(PARAM_TYPE);

        if (layerParam.isEmpty()) {
            return response;
        }

        var cas = aCas.get();
        var layer = findLayer(aVid, cas, layerParam, aDocument.getProject());

        var details = new ArrayList<VLazyDetailResult>();

        lookupLayerLevelDetails(aVid, cas, windowBeginOffset, windowEndOffset, layer)
                .forEach(details::add);

        for (var feature : annotationService.listAnnotationFeature(layer)) {
            lookupExtensionLevelDetails(aVid, aDocument, cas, aUser, feature).forEach(details::add);

            // FIXME: We would like to get feature-level lazy details for the annotation label
            // provided by the extension or said otherwise, we want to e.g. get KB details for a
            // concept
            // feature suggestion... this worked when we used the "query", but now is broken!
            lookupFeatureLevelDetail(aVid, cas, feature).forEach(details::add);
        }

        response.setResults(details.stream() //
                .map(d -> new LazyDetailQuery(d.getLabel(), d.getValue())) //
                .collect(toList()));

        return response;
    }

    private AnnotationLayer findLayer(VID aVid, CAS aCas, StringValue aLayerParam, Project project)
        throws AnnotationException, IOException
    {
        if (aVid.isSynthetic()) {
            var layerId = aLayerParam.toLong();
            return annotationService.getLayer(project, layerId)
                    .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                            + "] does not exist in project " + project));
        }

        var fs = selectFsByAddr(aCas, aVid.getId());
        return annotationService.findLayer(project, fs);
    }

    private List<VLazyDetailResult> lookupFeatureLevelDetail(VID aVid, CAS aCas,
            AnnotationFeature aFeature)
    {
        if (aVid.isSynthetic()) {
            return emptyList();
        }

        var fs = selectFsByAddr(aCas, aVid.getId());
        var ext = featureSupportRegistry.findExtension(aFeature).orElseThrow();
        return ext.lookupLazyDetails(aFeature, ext.getFeatureValue(aFeature, fs));
    }

    private List<VLazyDetailResult> lookupLayerLevelDetails(VID aVid, CAS aCas,
            int windowBeginOffset, int windowEndOffset, AnnotationLayer aLayer)
    {
        if (aVid.isSynthetic()) {
            return emptyList();
        }

        return layerSupportRegistry.getLayerSupport(aLayer)
                .createRenderer(aLayer, () -> annotationService.listAnnotationFeature(aLayer))
                .lookupLazyDetails(aCas, aVid, windowBeginOffset, windowEndOffset);

    }

    private List<VLazyDetailResult> lookupExtensionLevelDetails(VID aVid, SourceDocument aDocument,
            CAS aCas, User aUser, AnnotationFeature aFeature)
        throws IOException
    {
        if (!aVid.isSynthetic()) {
            return emptyList();
        }

        var result = new ArrayList<VLazyDetailResult>();
        var extension = extensionRegistry.getExtension(aVid.getExtensionId());
        var value = extension.getFeatureValue(aDocument, aUser, aCas, aVid, aFeature);
        featureSupportRegistry.findExtension(aFeature).orElseThrow()
                .lookupLazyDetails(aFeature, value).forEach(result::add);
        extension.lookupLazyDetails(aDocument, aUser, aVid, aFeature).forEach(result::add);
        return result;
    }
}
