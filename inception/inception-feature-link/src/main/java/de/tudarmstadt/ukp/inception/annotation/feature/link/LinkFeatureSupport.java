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
package de.tudarmstadt.ukp.inception.annotation.feature.link;

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink.toMaterializedLink;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.disjunction;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_TOP;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#slotFeatureSupport}.
 * </p>
 */
public class LinkFeatureSupport
    implements FeatureSupport<LinkFeatureTraits>
{
    public static final String FEATURE_NAME_TARGET = "target";
    public static final String FEATURE_NAME_ROLE = "role";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService annotationService;

    private String featureSupportId;

    @Autowired
    public LinkFeatureSupport(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public String getId()
    {
        return featureSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        featureSupportId = aBeanName;
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aLayer)
    {
        var types = new ArrayList<FeatureType>();

        // Slot features are only supported on span layers
        if (SpanLayerSupport.TYPE.equals(aLayer.getType())
                || LayerTypes.CHAIN_LAYER_TYPE.equals(aLayer.getType())) {
            // Add layers of type SPAN available in the project
            for (var layer : annotationService.listAnnotationLayer(aLayer.getProject())) {

                if (Token.class.getName().equals(layer.getName())
                        || Sentence.class.getName().equals(layer.getName())) {
                    continue;
                }

                if (SpanLayerSupport.TYPE.equals(layer.getType())) {
                    types.add(new FeatureType(layer.getName(), "Link: " + layer.getUiName(),
                            featureSupportId));
                }
            }

            // Also allow the user to use any annotation type as slot filler
            types.add(new FeatureType(CAS.TYPE_NAME_ANNOTATION, "Link: <Any>", featureSupportId));
        }

        return types;
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case ARRAY:
            switch (aFeature.getLinkMode()) {
            case WITH_ROLE:
                return true;
            default:
                return false;
            }
        case NONE: // fall-through
        default:
            return false;
        }
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new LinkFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        var feature = aFeatureStateModel.getObject().feature;
        final FeatureEditor editor;

        switch (feature.getMultiValueMode()) {
        case ARRAY:
            switch (feature.getLinkMode()) {
            case WITH_ROLE:
                editor = new LinkFeatureEditor(aId, aOwner, aHandler, aStateModel,
                        aFeatureStateModel);
                break;
            default:
                throw unsupportedFeatureTypeException(feature);
            }
            break;
        case NONE:
            throw unsupportedLinkModeException(feature);
        default:
            throw unsupportedMultiValueModeException(feature);
        }

        return editor;
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        // Set properties of link features since these are currently not configurable in the UI
        aFeature.setMode(ARRAY);
        aFeature.setLinkMode(WITH_ROLE);
        aFeature.setLinkTypeRoleFeatureName(FEATURE_NAME_ROLE);
        aFeature.setLinkTypeTargetFeatureName(FEATURE_NAME_TARGET);
        aFeature.setLinkTypeName(
                aFeature.getLayer().getName() + capitalize(aFeature.getName()) + "Link");
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        // Link type
        var linkTD = aTSD.addType(aFeature.getLinkTypeName(), "", TYPE_NAME_TOP);
        linkTD.addFeature(aFeature.getLinkTypeRoleFeatureName(), "", TYPE_NAME_STRING);
        linkTD.addFeature(aFeature.getLinkTypeTargetFeatureName(), "", aFeature.getType());

        // Link feature
        aTD.addFeature(aFeature.getName(), aFeature.getDescription(), TYPE_NAME_FS_ARRAY,
                linkTD.getName(), false);
    }

    @Override
    public List<LinkWithRoleModel> getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        var linkFeature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        if (linkFeature == null) {
            return emptyList();
        }

        return wrapFeatureValue(aFeature, aFS.getCAS(), aFS.getFeatureValue(linkFeature));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return (V) emptyList();
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        if (aValue instanceof List && aFeature.getTagset() != null) {
            for (var link : (List<LinkWithRoleModel>) aValue) {
                if (!annotationService.existsTag(link.role, aFeature.getTagset())) {
                    if (!aFeature.getTagset().isCreateTag()) {
                        throw new IllegalArgumentException("[" + link.role
                                + "] is not in the tag list. Please choose from the existing tags.");
                    }

                    if (isBlank(link.role)) {
                        continue;
                    }

                    var selectedTag = new Tag();
                    selectedTag.setName(link.role);
                    selectedTag.setTagSet(aFeature.getTagset());
                    annotationService.createTag(selectedTag);
                }
            }
        }

        FeatureSupport.super.setFeatureValue(aCas, aFeature, aAddress, aValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LinkWithRoleModel> unwrapFeatureValue(AnnotationFeature aFeature, Object aValue)
    {
        if (aValue instanceof List) {
            // This is not actually implemented because the setFeatureValue knows how to deal with
            // slot features. This only needs to be implemented when WebAnnoCasUtil.setLinkFeature
            // is moved into the slot feature support.
            return (List<LinkWithRoleModel>) aValue;
        }
        else if (aValue == null) {
            return null;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<LinkWithRoleModel> wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS,
            Object aValue)
    {
        if (aValue == null) {
            return new ArrayList<>();
        }

        FeatureStructure[] values = null;
        if (aValue instanceof ArrayFS array) {
            values = array.toArray();
        }

        if (aValue instanceof Collection) {
            values = ((Collection<FeatureStructure>) aValue).stream()
                    .toArray(FeatureStructure[]::new);
        }

        if (values == null) {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
        }

        var linkType = aCAS.getTypeSystem().getType(aFeature.getLinkTypeName());
        var roleFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeRoleFeatureName());
        var targetFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeTargetFeatureName());

        var links = new ArrayList<LinkWithRoleModel>(values.length);
        for (var link : values) {
            var m = new LinkWithRoleModel();
            m.role = link.getStringValue(roleFeat);
            m.targetAddr = ICasUtil.getAddr(link.getFeatureValue(targetFeat));
            m.label = ((AnnotationFS) link.getFeatureValue(targetFeat)).getCoveredText();
            links.add(m);
        }

        return links;
    }

    @Override
    public LinkFeatureTraits createDefaultTraits()
    {
        return new LinkFeatureTraits();
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aLabel)
    {
        // Never render link feature labels
        return null;
    }

    @Override
    public boolean isCopyOnCurationMerge(AnnotationFeature aFeature)
    {
        // Links count as separate positions and should be merged separately
        return false;
    }

    @Override
    public boolean isFeatureValueEqual(AnnotationFeature aFeature, FeatureStructure aFS1,
            FeatureStructure aFS2)
    {
        List<LinkWithRoleModel> links1 = getFeatureValue(aFeature, aFS1);
        var matLinks1 = links1.stream() //
                .map(link -> toMaterializedLink(aFS1, aFeature, link))//
                .toList();

        List<LinkWithRoleModel> links2 = getFeatureValue(aFeature, aFS2);
        var matLinks2 = links2.stream()//
                .map(link -> toMaterializedLink(aFS2, aFeature, link))//
                .toList();

        return disjunction(matLinks1, matLinks2).isEmpty();
    }
}
