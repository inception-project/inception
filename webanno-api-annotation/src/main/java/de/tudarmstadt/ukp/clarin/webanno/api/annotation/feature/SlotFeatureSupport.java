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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureTraitsEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component
public class SlotFeatureSupport
    implements FeatureSupport<Void>
{
    private @Autowired AnnotationSchemaService annotationService;

    private String featureSupportId;
    
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
        return new LinkFeatureTraitsEditor(aId, aFeatureModel);
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
    public boolean isTagsetSupported(AnnotationFeature aFeature)
    {
        return true;
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
    public <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        // Get type and features - we need them later in the loop
        Feature linkFeature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        Type linkType = aFS.getCAS().getTypeSystem().getType(aFeature.getLinkTypeName());
        Feature roleFeat = linkType.getFeatureByBaseName(aFeature
                .getLinkTypeRoleFeatureName());
        Feature targetFeat = linkType.getFeatureByBaseName(aFeature
                .getLinkTypeTargetFeatureName());

        List<LinkWithRoleModel> links = new ArrayList<>();
        ArrayFS array = (ArrayFS) aFS.getFeatureValue(linkFeature);
        if (array != null) {
            for (FeatureStructure link : array.toArray()) {
                LinkWithRoleModel m = new LinkWithRoleModel();
                m.role = link.getStringValue(roleFeat);
                m.targetAddr = WebAnnoCasUtil.getAddr(link.getFeatureValue(targetFeat));
                m.label = ((AnnotationFS) link.getFeatureValue(targetFeat))
                        .getCoveredText();
                links.add(m);
            }
        }
        return (T) links;
    }
}
