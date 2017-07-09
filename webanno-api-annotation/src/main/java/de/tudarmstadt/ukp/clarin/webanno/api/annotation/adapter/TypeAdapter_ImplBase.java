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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class TypeAdapter_ImplBase
    implements TypeAdapter
{
    private FeatureSupportRegistry featureSupportRegistry;
    
    private AnnotationLayer layer;

    private Map<String, AnnotationFeature> features;

    private boolean deletable;

    public TypeAdapter_ImplBase(FeatureSupportRegistry aFeatureSupportRegistry,
            AnnotationLayer aLayer, Collection<AnnotationFeature> aFeatures)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        layer = aLayer;

        // Using a sorted map here so we have reliable positions in the map when iterating. We use
        // these positions to remember the armed slots!
        features = new TreeMap<>();
        for (AnnotationFeature f : aFeatures) {
            features.put(f.getName(), f);
        }
    }
    
    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }
    
    @Override
    public Collection<AnnotationFeature> listFeatures()
    {
        return features.values();
    }
    
    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    @Override
    public boolean isDeletable()
    {
        return deletable;
    }

    @Override
    public void setFeatureValue(AnnotationFeature aFeature, JCas aJcas, int aAddress, Object aValue)
    {
        featureSupportRegistry.getFeatureSupport(aFeature).setFeatureValue(aJcas, aFeature,
                aAddress, aValue);
    }

    @Override
    public <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        return featureSupportRegistry.getFeatureSupport(aFeature).getFeatureValue(aFeature, aFs);
    }
}
