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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.text.WordUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
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
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureType;
import de.tudarmstadt.ukp.inception.schema.feature.LinkWithRoleModel;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#slotFeatureSupport}.
 * </p>
 */
public class LinkFeatureSupport
    implements FeatureSupport<LinkFeatureTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

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
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        List<FeatureType> types = new ArrayList<>();

        // Slot features are only supported on span layers
        if (!WebAnnoConst.CHAIN_TYPE.equals(aAnnotationLayer.getType())
                && !WebAnnoConst.RELATION_TYPE.equals(aAnnotationLayer.getType())) {
            // Add layers of type SPAN available in the project
            for (AnnotationLayer spanLayer : annotationService
                    .listAnnotationLayer(aAnnotationLayer.getProject())) {

                if (Token.class.getName().equals(spanLayer.getName())
                        || Sentence.class.getName().equals(spanLayer.getName())) {
                    continue;
                }

                if (WebAnnoConst.SPAN_TYPE.equals(spanLayer.getType())) {
                    types.add(new FeatureType(spanLayer.getName(), "Link: " + spanLayer.getUiName(),
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
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;
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
        aFeature.setMode(MultiValueMode.ARRAY);
        aFeature.setLinkMode(LinkMode.WITH_ROLE);
        aFeature.setLinkTypeRoleFeatureName("role");
        aFeature.setLinkTypeTargetFeatureName("target");
        aFeature.setLinkTypeName(
                aFeature.getLayer().getName() + WordUtils.capitalize(aFeature.getName()) + "Link");
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        // Link type
        TypeDescription linkTD = aTSD.addType(aFeature.getLinkTypeName(), "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature(aFeature.getLinkTypeRoleFeatureName(), "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature(aFeature.getLinkTypeTargetFeatureName(), "", aFeature.getType());

        // Link feature
        aTD.addFeature(aFeature.getName(), aFeature.getDescription(), CAS.TYPE_NAME_FS_ARRAY,
                linkTD.getName(), false);
    }

    @Override
    public List<LinkWithRoleModel> getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Feature linkFeature = aFS.getType().getFeatureByBaseName(aFeature.getName());

        if (linkFeature == null) {
            wrapFeatureValue(aFeature, aFS.getCAS(), null);
        }

        return wrapFeatureValue(aFeature, aFS.getCAS(), aFS.getFeatureValue(linkFeature));
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        if (aValue instanceof List && aFeature.getTagset() != null) {
            for (LinkWithRoleModel link : (List<LinkWithRoleModel>) aValue) {
                if (!annotationService.existsTag(link.role, aFeature.getTagset())) {
                    if (!aFeature.getTagset().isCreateTag()) {
                        throw new IllegalArgumentException("[" + link.role
                                + "] is not in the tag list. Please choose from the existing tags");
                    }

                    Tag selectedTag = new Tag();
                    selectedTag.setName(link.role);
                    selectedTag.setTagSet(aFeature.getTagset());
                    annotationService.createTag(selectedTag);
                }
            }
        }

        FeatureSupport.super.setFeatureValue(aCas, aFeature, aAddress, aValue);
    }

    @Override
    public List<LinkWithRoleModel> unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS,
            Object aValue)
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
        if (aValue instanceof ArrayFS) {
            values = ((ArrayFS<?>) aValue).toArray();
        }

        if (aValue instanceof Collection) {
            values = ((Collection<FeatureStructure>) aValue).stream()
                    .toArray(FeatureStructure[]::new);
        }

        if (values == null) {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
        }

        Type linkType = aCAS.getTypeSystem().getType(aFeature.getLinkTypeName());
        Feature roleFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeRoleFeatureName());
        Feature targetFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeTargetFeatureName());

        ArrayList<LinkWithRoleModel> links = new ArrayList<>();
        for (FeatureStructure link : values) {
            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = link.getStringValue(roleFeat);
            m.targetAddr = ICasUtil.getAddr(link.getFeatureValue(targetFeat));
            m.label = ((AnnotationFS) link.getFeatureValue(targetFeat)).getCoveredText();
            links.add(m);
        }

        return links;
    }

    @Override
    public LinkFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        LinkFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(LinkFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new LinkFeatureTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, LinkFeatureTraits aTraits)
    {
        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            log.error("Unable to write traits", e);
        }
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aLabel)
    {
        // Never render link feature labels
        return null;
    }
}
