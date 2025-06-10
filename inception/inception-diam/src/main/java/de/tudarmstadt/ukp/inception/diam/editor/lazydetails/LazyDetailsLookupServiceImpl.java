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

import static de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler.PARAM_LAYER_ID;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.string.StringValue;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

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
    public List<LazyDetailGroup> lookupLazyDetails(IRequestParameters request, VID aVid,
            CasProvider aCas, SourceDocument aDocument, User aDataOwner, int windowBeginOffset,
            int windowEndOffset)
        throws AnnotationException, IOException
    {
        var layerParam = request.getParameterValue(PARAM_LAYER_ID);

        if (layerParam.isEmpty()) {
            return emptyList();
        }

        var cas = aCas.get();

        var detailGroups = lookLazyDetails(aVid, aDocument, aDataOwner, layerParam, cas);

        return detailGroups.stream() //
                .map(this::toExternalForm) //
                .collect(toList());
    }

    private List<VLazyDetailGroup> lookLazyDetails(VID aVid, SourceDocument aDocument,
            User aDataOwner, StringValue aLayerParam, CAS aCas)
        throws AnnotationException, IOException
    {
        if (isSentence(aCas, aVid)) {
            return lookupSentenceLevelLazyDetails(aVid, aCas);
        }

        var layer = findLayer(aVid, aCas, aLayerParam, aDocument.getProject());
        return lookupAnnotationLevelDetails(aVid, aDocument, aDataOwner, layer, aCas);
    }

    @Override
    public List<VLazyDetailGroup> lookupAnnotationLevelDetails(VID aVid, SourceDocument aDocument,
            User aDataOwner, AnnotationLayer aLayer, CAS aCas)
        throws AnnotationException, IOException
    {
        var detailGroups = new ArrayList<VLazyDetailGroup>();

        lookupLayerLevelDetails(aVid, aCas, aLayer).forEach(detailGroups::add);

        if (aVid.isSynthetic()) {
            lookupExtensionLevelDetails(aVid, aDocument, aCas, aDataOwner, aLayer)
                    .forEach(detailGroups::add);
        }
        else {
            for (var feature : annotationService.listSupportedFeatures(aLayer)) {
                lookupFeatureLevelDetails(aVid, aCas, feature).forEach(detailGroups::add);
            }
        }

        return detailGroups;
    }

    private List<VLazyDetailGroup> lookupSentenceLevelLazyDetails(VID aVid, CAS cas)
    {
        var detailGroups = new ArrayList<VLazyDetailGroup>();
        var fs = selectFsByAddr(cas, aVid.getId());
        var id = FSUtil.getFeature(fs, Sentence._FeatName_id, String.class);
        if (StringUtils.isNotBlank(id)) {
            var group = new VLazyDetailGroup();
            group.addDetail(new VLazyDetail(Sentence._FeatName_id, id));
            detailGroups.add(group);
        }

        return detailGroups;
    }

    private boolean isSentence(CAS aCas, VID aVid)
    {
        if (aVid.isSynthetic()) {
            return false;
        }

        var fs = selectFsByAddr(aCas, aVid.getId());
        return Sentence._TypeName.equals(fs.getType().getName());

    }

    private LazyDetailGroup toExternalForm(VLazyDetailGroup aGroup)
    {
        var extGroup = new LazyDetailGroup(aGroup.getTitle());
        extGroup.setDetails(aGroup.getDetails().stream()
                .map(d -> new LazyDetail(d.getLabel(), d.getValue())).collect(toList()));
        return extGroup;
    }

    private AnnotationLayer findLayer(VID aVid, CAS aCas, StringValue aLayerParam, Project aProject)
        throws AnnotationException, IOException
    {
        if (aVid.isSynthetic()) {
            var layerId = aLayerParam.toLong();
            return annotationService.getLayer(aProject, layerId)
                    .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                            + "] does not exist in project " + aProject));
        }

        var fs = selectFsByAddr(aCas, aVid.getId());
        return annotationService.findLayer(aProject, fs);
    }

    @Override
    public List<VLazyDetailGroup> lookupFeatureLevelDetails(VID aVid, CAS aCas,
            AnnotationFeature aFeature)
    {
        if (aVid.isSynthetic()) {
            return emptyList();
        }

        var fs = selectFsByAddr(aCas, aVid.getId());
        var ext = featureSupportRegistry.findExtension(aFeature).orElseThrow();
        if (!ext.isAccessible(aFeature)) {
            return emptyList();
        }

        return ext.lookupLazyDetails(aFeature, ext.getFeatureValue(aFeature, fs));
    }

    @Override
    public List<VLazyDetailGroup> lookupLayerLevelDetails(VID aVid, CAS aCas,
            AnnotationLayer aLayer)
    {
        if (aVid.isSynthetic()) {
            return emptyList();
        }

        return layerSupportRegistry.getLayerSupport(aLayer)
                .createRenderer(aLayer, () -> annotationService.listAnnotationFeature(aLayer))
                .lookupLazyDetails(aCas, aVid);

    }

    private List<VLazyDetailGroup> lookupExtensionLevelDetails(VID aVid, SourceDocument aDocument,
            CAS aCas, User aDataOwner, AnnotationLayer aLayer)
        throws IOException
    {
        if (!aVid.isSynthetic()) {
            return emptyList();
        }

        var extension = extensionRegistry.getExtension(aVid.getExtensionId());
        if (extension == null) {
            return emptyList();
        }

        return extension.lookupLazyDetails(aDocument, aDataOwner, aCas, aVid, aLayer);
    }
}
