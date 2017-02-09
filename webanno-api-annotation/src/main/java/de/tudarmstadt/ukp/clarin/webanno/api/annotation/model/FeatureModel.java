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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

public class FeatureModel
    implements Serializable
{
    private static final long serialVersionUID = 3512979848975446735L;
    public final AnnotationFeature feature;
    public Serializable value;

    public FeatureModel(AnnotationFeature aFeature, Serializable aValue)
    {
        feature = aFeature;
        value = aValue;

        // Avoid having null here because otherwise we have to handle null in zillion places!
        if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
            value = new ArrayList<>();
        }
    }
}