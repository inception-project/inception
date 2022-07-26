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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IObjectClassAwareModel;
import org.apache.wicket.util.lang.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;

public class FeatureStateModel
    implements IModel<FeatureState>, IObjectClassAwareModel<FeatureState>
{
    private static final long serialVersionUID = 1L;

    private final IModel<AnnotatorState> state;
    private final AnnotationFeature feature;

    public FeatureStateModel(final IModel<AnnotatorState> aState, final AnnotationFeature aFeature)
    {
        state = aState;
        feature = aFeature;
    }

    @Override
    public FeatureState getObject()
    {
        return state.getObject().getFeatureState(feature);
    }

    @Override
    public void setObject(FeatureState object)
    {
        FeatureState fm = state.getObject().getFeatureState(feature);
        fm.value = object.value;
        // Probably don't need to copy the other fields
    }

    /**
     * @see org.apache.wicket.model.IDetachable#detach()
     */
    @Override
    public void detach()
    {
        // Do nothing.
    }

    public static FeatureStateModel of(final IModel<AnnotatorState> aState,
            FeatureState aFeatureModel)
    {
        return new FeatureStateModel(aState, aFeatureModel.feature);
    }

    @Override
    public int hashCode()
    {
        return feature.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FeatureStateModel)) {
            return false;
        }
        FeatureStateModel that = (FeatureStateModel) obj;
        return Objects.equal(feature, that.feature);
    }

    @Override
    public Class<FeatureState> getObjectClass()
    {
        return FeatureState.class;
    }
}
