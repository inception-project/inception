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
package de.tudarmstadt.ukp.inception.annotation.feature.number;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#numberFeatureSupport}.
 * </p>
 */
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
        var feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        var traits = readTraits(feature);

        switch (feature.getType()) {
        case CAS.TYPE_NAME_INTEGER: {
            if (traits.getEditorType().equals(NumberFeatureTraits.EditorType.RADIO_BUTTONS)) {
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
        // Numeric features cannot have a tagset
        aFeature.setTagset(null);

        // Numeric features cannot be null
        aFeature.setRequired(true);
    }

    @Override
    public NumberFeatureTraits createDefaultTraits()
    {
        return new NumberFeatureTraits();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return switch (aFeature.getType()) {
        case CAS.TYPE_NAME_INTEGER -> (V) (Object) 0;
        case CAS.TYPE_NAME_FLOAT -> (V) (Object) 0.0f;
        default -> throw unsupportedFeatureTypeException(aFeature);
        };
    }
}
