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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureTraitsEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component
public class SlotFeatureSupport
    implements FeatureSupport<LinkFeatureTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final AnnotationSchemaService annotationService;

    private String featureSupportId;
    
    @Autowired
    public SlotFeatureSupport(AnnotationSchemaService aAnnotationService)
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
        if (WebAnnoConst.SPAN_TYPE.equals(aAnnotationLayer.getType())) {
            // Add layers of type SPAN available in the project
            for (AnnotationLayer spanLayer : annotationService
                    .listAnnotationLayer(aAnnotationLayer.getProject())) {
                
                if (
                        Token.class.getName().equals(spanLayer.getName()) || 
                        Sentence.class.getName().equals(spanLayer.getName())) 
                {
                    continue;
                }

                if (WebAnnoConst.SPAN_TYPE.equals(spanLayer.getType())) {
                    types.add(new FeatureType(spanLayer.getName(), 
                            "Link: " + spanLayer.getUiName(), featureSupportId));
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
    public Panel createTraitsEditor(String aId,  IModel<AnnotationFeature> aFeatureModel)
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
        aFeature.setLinkTypeName(aFeature.getLayer().getName()
                + WordUtils.capitalize(aFeature.getName()) + "Link");
    }
    
    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        // Link type
        TypeDescription linkTD = aTSD.addType(aFeature.getLinkTypeName(), "",
                CAS.TYPE_NAME_TOP);
        linkTD.addFeature(aFeature.getLinkTypeRoleFeatureName(), "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature(aFeature.getLinkTypeTargetFeatureName(), "", aFeature.getType());
        // Link feature
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(),
                false);
    }
    
    @Override
    public List<LinkWithRoleModel> getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Feature linkFeature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        return wrapFeatureValue(aFeature, aFS.getCAS(), aFS.getFeatureValue(linkFeature));
    }
    
    @Override
    public void setFeatureValue(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        if (
                aValue instanceof List &&
                aFeature.getTagset() != null
        ) {
            for (LinkWithRoleModel link : (List<LinkWithRoleModel>) aValue) {
                if (!annotationService.existsTag(link.role, aFeature.getTagset())) {
                    if (!aFeature.getTagset().isCreateTag()) {
                        throw new IllegalArgumentException("[" + link.role
                                + "] is not in the tag list. Please choose from the existing tags");
                    }
                    else {
                        Tag selectedTag = new Tag();
                        selectedTag.setName(link.role);
                        selectedTag.setTagSet(aFeature.getTagset());
                        annotationService.createTag(selectedTag);
                    }
                }
            }
        }
        
        FeatureSupport.super.setFeatureValue(aJcas, aFeature, aAddress, aValue);
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
    
    @Override
    public List<LinkWithRoleModel> wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS,
            Object aValue)
    {
        if (aValue instanceof ArrayFS) {
            ArrayFS array = (ArrayFS) aValue;

            Type linkType = aCAS.getTypeSystem().getType(aFeature.getLinkTypeName());
            Feature roleFeat = linkType.getFeatureByBaseName(aFeature
                    .getLinkTypeRoleFeatureName());
            Feature targetFeat = linkType.getFeatureByBaseName(aFeature
                    .getLinkTypeTargetFeatureName());

            List<LinkWithRoleModel> links = new ArrayList<>();
            for (FeatureStructure link : array.toArray()) {
                LinkWithRoleModel m = new LinkWithRoleModel();
                m.role = link.getStringValue(roleFeat);
                m.targetAddr = WebAnnoCasUtil.getAddr(link.getFeatureValue(targetFeat));
                m.label = ((AnnotationFS) link.getFeatureValue(targetFeat))
                        .getCoveredText();
                links.add(m);
            }
            
            return links;
        }
        else if (aValue == null ) {
            return new ArrayList<>();
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
        }
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
}
