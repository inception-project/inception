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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface FeatureSupport
{
    List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer);

    boolean accepts(AnnotationFeature aFeature);

    FeatureEditor createEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
            IModel<AnnotatorState> aStateModel, IModel<FeatureState> aFeatureStateModel);

    /**
     * Gets the label that should be displayed for the given feature value in the UI.
     * {@code null} is an acceptable return value for this method.
     */
    default String renderFeatureValue(AnnotationFeature aFeature, AnnotationFS aFs,
            Feature aLabelFeature)
    {
        return aFs.getFeatureValueAsString(aLabelFeature);
    }
    
    /**
     * Update this feature with a new value. This method should not be called directly but
     * rather via {@link TypeAdapter#updateFeature}.
     *
     * @param aJcas
     *            the JCas.
     * @param aFeature
     *            the feature.
     * @param aAddress
     *            the annotation ID.
     * @param aValue
     *            the value.
     */
    default void setFeatureValue(JCas aJcas, AnnotationFeature aFeature, int aAddress,
            Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, aValue);
    }
    
    default public <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());

        switch (aFeature.getMultiValueMode()) {
        case NONE: {
            final String effectiveType;
            if (aFeature.isVirtualFeature()) {
                effectiveType = CAS.TYPE_NAME_STRING;
            }
            else {
                effectiveType = aFeature.getType();
            }
            
            // Sanity check
            if (!Objects.equals(effectiveType, feature.getRange().getName())) {
                throw new IllegalArgumentException("Actual feature type ["
                        + feature.getRange().getName() + "]does not match expected feature type ["
                        + effectiveType + "].");
            }

            // switch (aFeature.getType()) {
            // case CAS.TYPE_NAME_STRING:
            // return (T) aFS.getStringValue(feature);
            // case CAS.TYPE_NAME_BOOLEAN:
            // return (T) (Boolean) aFS.getBooleanValue(feature);
            // case CAS.TYPE_NAME_FLOAT:
            // return (T) (Float) aFS.getFloatValue(feature);
            // case CAS.TYPE_NAME_INTEGER:
            // return (T) (Integer) aFS.getIntValue(feature);
            // default:
            // throw new IllegalArgumentException("Cannot get value of feature ["
            // + aFeature.getName() + "] with type [" + feature.getRange().getName() + "]");
            // }
            return WebAnnoCasUtil.getFeature(aFS, aFeature.getName());
        }
        case ARRAY: {
            switch (aFeature.getLinkMode()) {
            case WITH_ROLE: {
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
            default:
                throw new IllegalArgumentException("Cannot get value of feature ["
                        + aFeature.getName() + "] with link mode [" + aFeature.getMultiValueMode()
                        + "]");
            }
        }
        default:
            throw new IllegalArgumentException("Unsupported multi-value mode ["
                    + aFeature.getMultiValueMode() + "] on feature [" + aFeature.getName() + "]");
        }
    }
    
    default IllegalArgumentException unsupportedFeatureTypeException(FeatureState aFeatureState)
    {
        return new IllegalArgumentException("Unsupported type [" + aFeatureState.feature.getType()
                + "] on feature [" + aFeatureState.feature.getName() + "]");
    }

    default IllegalArgumentException unsupportedLinkModeException(FeatureState aFeatureState)
    {
        return new IllegalArgumentException(
                "Unsupported link mode [" + aFeatureState.feature.getLinkMode() + "] on feature ["
                        + aFeatureState.feature.getName() + "]");
    }
    
    default IllegalArgumentException unsupportedMultiValueModeException(FeatureState aFeatureState)
    {
        return new IllegalArgumentException(
                "Unsupported multi-value mode [" + aFeatureState.feature.getMultiValueMode()
                        + "] on feature [" + aFeatureState.feature.getName() + "]");
    }
}
