/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

public class FeatureState
    implements Serializable
{
    private static final long serialVersionUID = 3512979848975446735L;
    public final AnnotationFeature feature;
    public Serializable value;
    public List<Tag> tagset;
    public List<PossibleValue> possibleValues;
    public RulesIndicator indicator = new RulesIndicator();

    public FeatureState(AnnotationFeature aFeature, Serializable aValue)
    {
        feature = aFeature;
        value = aValue;

        indicator.reset(); // reset the indicator

        // Avoid having null here because otherwise we have to handle null in zillion places!
        if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
            value = new ArrayList<>();
        }
    }

    // Two feature models are considered equal if they apply to the same feature. The value and
    // other properties are excluded. This is important so that the CachingReuseStrategy used by
    // FeatureEditorPanelContent can properly match its RefreshingView items to the models which
    // in turn is required such that we can send a "read" signal to Kendo ComboBoxes to refresh
    // their dropdowns when constraints are active.
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FeatureState other = (FeatureState) obj;
        if (feature == null) {
            if (other.feature != null) {
                return false;
            }
        }
        else if (!feature.equals(other.feature)) {
            return false;
        }
        return true;
    }
}