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

import javax.annotation.Resource;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class SlotFeatureSupport
    implements FeatureSupport
{
    private @Resource AnnotationSchemaService annotationService;

    public SlotFeatureSupport(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }
    
    @Override
    public List<String> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        List<String> types = new ArrayList<>();
        if (aAnnotationLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
            // Add layers of type SPAN available in the project
            for (AnnotationLayer spanLayer : annotationService
                    .listAnnotationLayer(aAnnotationLayer.getProject())) {
                if (spanLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                    types.add(spanLayer.getName());
                }
            }
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
        case NONE: // fallthrough
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
        case ARRAY:
            switch (featureState.feature.getLinkMode()) {
            case WITH_ROLE:
                editor = new LinkFeatureEditor(aId, aOwner, aHandler, aStateModel,
                        aFeatureStateModel);
                break;
            default:
                throw unsupportedFeatureTypeException(featureState);
            }
            break;
        case NONE:
            throw unsupportedLinkModeException(featureState);
        default:
            throw unsupportedMultiValueModeException(featureState);
        }
        
        return editor;
    }
}
