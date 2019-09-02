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
import java.util.Objects;

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
    public VID vid;

    public FeatureState(VID aVid, AnnotationFeature aFeature, Serializable aValue)
    {
        vid = aVid;
        feature = aFeature;
        value = aValue;

        indicator.reset(); // reset the indicator

        // Avoid having null here because otherwise we have to handle null in zillion places!
        if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
            value = new ArrayList<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureState that = (FeatureState) o;
        return feature.equals(that.feature) &&
                vid.equals(that.vid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, vid);
    }
}
