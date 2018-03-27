/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface ClassificationToolFactory<T>
{
    /**
     * Get the ID of the classification tool.
     */
    String getId();
    
    /**
     * Get the name of the classification tool (a human readable name).
     */
    String getName();
    
    ClassificationTool<T> createTool(long aRecommenderId, String aFeature, AnnotationLayer aLayer,
        int aMaxPredictions);

    boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature);
}
