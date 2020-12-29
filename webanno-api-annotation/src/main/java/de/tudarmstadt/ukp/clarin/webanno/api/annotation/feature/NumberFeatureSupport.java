/*
 * Copyright 2019
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.NumberFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.NumberFeatureTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.NumberFeatureTraitsEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.RatingFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Component
public class NumberFeatureSupport
    extends UimaPrimitiveFeatureSupport_ImplBase<NumberFeatureTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<FeatureType> primitiveTypes;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        primitiveTypes = asList(
                new FeatureType(CAS.TYPE_NAME_INTEGER, "Primitive: Integer", getId()),
                new FeatureType(CAS.TYPE_NAME_FLOAT, "Primitive: Float", getId()));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return Collections.unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        return MultiValueMode.NONE.equals(aFeature.getMultiValueMode())
                && (CAS.TYPE_NAME_INTEGER.equals(aFeature.getType())
                        || CAS.TYPE_NAME_FLOAT.equals(aFeature.getType()));
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        AnnotationFeature feature = aFeatureModel.getObject();

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        return new NumberFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        NumberFeatureTraits traits = readTraits(feature);

        switch (feature.getType()) {
        case CAS.TYPE_NAME_INTEGER: {
            if (traits.getEditorType().equals(NumberFeatureTraits.EDITOR_TYPE.RADIO_BUTTONS)) {
                int min = (int) traits.getMinimum();
                int max = (int) traits.getMaximum();
                List<Integer> range = IntStream.range(min, max + 1).boxed()
                        .collect(Collectors.toList());
                return new RatingFeatureEditor(aId, aOwner, aFeatureStateModel, range);
            }
            else {
                return new NumberFeatureEditor(aId, aOwner, aFeatureStateModel, traits);
            }
        }
        case CAS.TYPE_NAME_FLOAT: {
            return new NumberFeatureEditor(aId, aOwner, aFeatureStateModel, traits);
        }
        default:
            throw unsupportedFeatureTypeException(feature);
        }
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        // If the feature is not a string feature, force the tagset to null.
        aFeature.setTagset(null);
    }

    // TODO: trait reading/writing needs to be handled in another way to avoid duplicate code
    @Override
    public NumberFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        NumberFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(NumberFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new NumberFeatureTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, NumberFeatureTraits aTraits)
    {
        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            log.error("Unable to write traits", e);
        }
    }
}
