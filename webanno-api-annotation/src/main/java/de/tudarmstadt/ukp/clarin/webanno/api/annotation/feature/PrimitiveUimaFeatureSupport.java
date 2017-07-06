/*
 * Copyright 2017
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

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.BooleanFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.NumberFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.TextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class PrimitiveUimaFeatureSupport
    implements FeatureSupport
{
    private final static List<FeatureType> PRIMITIVE_TYPES = asList(
            new FeatureType(CAS.TYPE_NAME_STRING, "Primitive: String"),
            new FeatureType(CAS.TYPE_NAME_INTEGER, "Primitive: Integer"), 
            new FeatureType(CAS.TYPE_NAME_FLOAT, "Primitive: Float"), 
            new FeatureType(CAS.TYPE_NAME_BOOLEAN, "Primitive: Boolean"));

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return PRIMITIVE_TYPES;
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            switch (aFeature.getType()) {
            case CAS.TYPE_NAME_INTEGER: // fallthrough
            case CAS.TYPE_NAME_FLOAT: // fallthrough
            case CAS.TYPE_NAME_BOOLEAN: // fallthrough
            case CAS.TYPE_NAME_STRING: 
                return true;
            default:
                return false;
            }
        case ARRAY: // fallthrough
        default:
            return false;
        }
    }
    
    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        FeatureState featureState = aFeatureStateModel.getObject();
        final FeatureEditor editor;
        
        switch (featureState.feature.getMultiValueMode()) {
        case NONE:
            switch (featureState.feature.getType()) {
            case CAS.TYPE_NAME_INTEGER: {
                editor = new NumberFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            case CAS.TYPE_NAME_FLOAT: {
                editor = new NumberFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            case CAS.TYPE_NAME_BOOLEAN: {
                editor = new BooleanFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            case CAS.TYPE_NAME_STRING: {
                editor = new TextFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            default:
                throw unsupportedFeatureTypeException(featureState);
            }
            break;
        case ARRAY: // fallthrough
            throw unsupportedLinkModeException(featureState);
        default:
            throw unsupportedMultiValueModeException(featureState);
        }
        return editor;
    }
}
