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
package de.tudarmstadt.ukp.inception.kb.factlinking.feature;

import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.OBJECT_LINK;
import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.OBJECT_ROLE;
import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.QUALIFIER_LINK;
import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.SUBJECT_LINK;
import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.SUBJECT_ROLE;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.factlinking.config.FactLinkingAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * To create feature support for subject and object of the fact layer
 * <p>
 * This class is exposed as a Spring Component via
 * {@link FactLinkingAutoConfiguration#subjectObjectFeatureSupport}.
 * </p>
 * 
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Deprecated
public class SubjectObjectFeatureSupport
    implements FeatureSupport<Void>
{
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
        return new ArrayList<>();
    }

    @Override
    public boolean accepts(AnnotationFeature annotationFeature)
    {
        switch (annotationFeature.getMultiValueMode()) {
        case ARRAY:
            switch (annotationFeature.getLinkMode()) {
            case WITH_ROLE:
                switch (annotationFeature.getLinkTypeName()) {
                case SUBJECT_LINK: // fall-through
                case OBJECT_LINK: // fall-through
                case QUALIFIER_LINK:
                    return true;
                default:
                    return false;
                }
            default:
                return false;
            }
        case NONE: // fall-through
        default:
            return false;
        }
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel)
    {

        FeatureState featureState = aFeatureStateModel.getObject();
        final FeatureEditor editor;

        switch (featureState.feature.getMultiValueMode()) {
        case ARRAY:
            switch (featureState.feature.getLinkMode()) {
            case WITH_ROLE:
                switch (featureState.feature.getLinkTypeName()) {
                case SUBJECT_LINK:
                    editor = new SubjectObjectFeatureEditor(aId, aOwner, aHandler, aStateModel,
                            aFeatureStateModel, SUBJECT_ROLE);
                    break;
                case OBJECT_LINK:
                    editor = new SubjectObjectFeatureEditor(aId, aOwner, aHandler, aStateModel,
                            aFeatureStateModel, OBJECT_ROLE);
                    break;
                case QUALIFIER_LINK:
                    editor = new QualifierFeatureEditor(aId, aOwner, aHandler, aStateModel,
                            aFeatureStateModel);
                    break;
                default:
                    throw unsupportedLinkModeException(featureState.feature);
                }
                break;
            default:
                throw unsupportedMultiValueModeException(featureState.feature);
            }
            break;
        case NONE:
            throw unsupportedFeatureTypeException(featureState.feature);
        default:
            throw unsupportedMultiValueModeException(featureState.feature);
        }
        return editor;
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
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
    }

    @Override
    public <V> V getDefaultFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LinkWithRoleModel> getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Feature linkFeature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        return wrapFeatureValue(aFeature, aFS.getCAS(), aFS.getFeatureValue(linkFeature));
    }

    @SuppressWarnings("unchecked")
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
    public ArrayList<LinkWithRoleModel> wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS,
            Object aValue)
    {
        if (aValue instanceof ArrayFS) {
            @SuppressWarnings("unchecked")
            var array = (ArrayFS<FeatureStructure>) aValue;

            Type linkType = aCAS.getTypeSystem().getType(aFeature.getLinkTypeName());
            Feature roleFeat = linkType.getFeatureByBaseName(aFeature.getLinkTypeRoleFeatureName());
            Feature targetFeat = linkType
                    .getFeatureByBaseName(aFeature.getLinkTypeTargetFeatureName());

            ArrayList<LinkWithRoleModel> links = new ArrayList<>();
            for (FeatureStructure link : array.toArray()) {
                LinkWithRoleModel m = new LinkWithRoleModel();
                m.role = link.getStringValue(roleFeat);
                m.targetAddr = ICasUtil.getAddr(link.getFeatureValue(targetFeat));
                m.label = ((AnnotationFS) link.getFeatureValue(targetFeat)).getCoveredText();
                links.add(m);
            }

            return links;
        }
        else if (aValue == null) {
            return new ArrayList<>();
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
        }
    }
}
