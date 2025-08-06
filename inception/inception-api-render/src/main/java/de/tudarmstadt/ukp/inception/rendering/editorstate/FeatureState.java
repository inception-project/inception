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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class FeatureState
    implements Serializable
{
    private static final long serialVersionUID = 3512979848975446735L;

    public final AnnotationFeature feature;
    public final VID vid;
    public final RulesIndicator indicator = new RulesIndicator();

    public Serializable value;
    public List<ReorderableTag> tagset;
    public List<PossibleValue> possibleValues;
    public List<SuggestionState> suggestionInfos = emptyList();

    public FeatureState(VID aVid, AnnotationFeature aFeature, Serializable aValue)
    {
        vid = aVid;
        feature = aFeature;
        value = aValue;

        indicator.reset(); // reset the indicator

        // Avoid having null here because otherwise we have to handle null in gadzillion places!
        if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
            value = new ArrayList<>();
        }
    }

    public void setSuggestions(List<SuggestionState> aSuggestionInfos)
    {
        suggestionInfos = aSuggestionInfos;
    }

    public List<SuggestionState> getSuggestions()
    {
        return suggestionInfos;
    }

    public Serializable getValue()
    {
        return value;
    }

    public void setValue(Serializable aValue)
    {
        value = aValue;
    }

    public VID getVid()
    {
        return vid;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureState that = (FeatureState) o;
        return feature.equals(that.feature) && vid.equals(that.vid);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(feature, vid);
    }
}
