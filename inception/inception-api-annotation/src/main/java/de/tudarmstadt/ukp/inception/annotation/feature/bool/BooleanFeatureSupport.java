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
package de.tudarmstadt.ukp.inception.annotation.feature.bool;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#booleanFeatureSupport}.
 * </p>
 */
public class BooleanFeatureSupport
    extends UimaPrimitiveFeatureSupport_ImplBase<Void>
{
    private List<FeatureType> primitiveTypes;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        primitiveTypes = asList(
                new FeatureType(CAS.TYPE_NAME_BOOLEAN, "Primitive: Boolean", getId()));
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
                && CAS.TYPE_NAME_BOOLEAN.equals(aFeature.getType());
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

        return new BooleanFeatureEditor(aId, aOwner, aFeatureStateModel);
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        // If the feature is not a string feature, force the tagset to null.
        aFeature.setTagset(null);
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aLabel)
    {
        if (aLabel == null) {
            return super.renderFeatureValue(aFeature, aLabel);
        }

        if ("true".equals(aLabel)) {
            return "+" + aFeature.getUiName();
        }
        else {
            return "-" + aFeature.getUiName();
        }
    }
}
