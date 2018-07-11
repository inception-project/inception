/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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

package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class NamedEntityTrainer
    extends Trainer<Object>
{
    public NamedEntityTrainer(ClassifierConfiguration<Object> configuration)
    {
        super(configuration);
    }

    @Override
    public void reconfigure()
    {
        // Nothing to do here atm.
    }

    /**
     *
     * @param trainingDataIncrement The current batch of training data.
     * @return A set of all AnnotationObjects with NamedEntity annotations on the value feature
     */
    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        Set<AnnotationObject> annotations = new HashSet<>();
        trainingDataIncrement.forEach(annotations::addAll);
        return annotations.stream()
            .filter(a -> a.getLabel() != null)
            .filter(a -> a.getFeature().equals("value"))
            .collect(Collectors.toSet());
    }

    @Override
    public boolean saveModel()
    {
        return false;
    }

    @Override
    public Object loadModel()
    {
        return null;
    }

}
