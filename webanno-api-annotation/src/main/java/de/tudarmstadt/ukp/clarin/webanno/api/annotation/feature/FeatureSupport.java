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

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface FeatureSupport
{
    List<String> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer);

    boolean accepts(AnnotationFeature aFeature);

    FeatureEditor createEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
            final IModel<AnnotatorState> aStateModel, final IModel<FeatureState> aFeatureStateModel);

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
