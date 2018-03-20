/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * To create feature support for subject and object of the fact layer
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class SubjectObjectFeatureSupport
    implements FeatureSupport
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SUBJECT_LINK = "webanno.custom.FactSubjectLink";
    private static final String OBJECT_LINK = "webanno.custom.FactObjectLink";
    private static final String MODIFIER_LINK = "webanno.custom.FactModifierLink";
    private static final String SUBJECT_ROLE = "subject";
    private static final String OBJECT_ROLE = "object";

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
                case SUBJECT_LINK: // fallthrough
                case OBJECT_LINK: // fallthrough
                case MODIFIER_LINK:
                    return true;
                default:
                    return false;
                }
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
//                case MODIFIER_LINK:
//                    editor = new ModifierFeatureEditor(aId, aOwner, aHandler, aStateModel,
//                        aFeatureStateModel);
//                    break;
                default:
                    throw unsupportedLinkModeException(featureState);
                }
                break;
            default:
                throw unsupportedMultiValueModeException(featureState);
            }
            break;
        case NONE:
            throw unsupportedFeatureTypeException(featureState);
        default:
            throw unsupportedMultiValueModeException(featureState);
        }
        return editor;
    }

}
