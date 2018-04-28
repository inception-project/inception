/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.api;

import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

/**
 * This class defines the methods of a trainer. Every trainer used in a {@link ClassificationTool}
 * class has to implement the defined abstract methods.
 * 
 *
 *
 * @param <T>
 *            The classifier configuration used to configure this trainer.
 */
public abstract class Trainer<T>
    extends ConfigurableComponent<T>
{
    public Trainer(ClassifierConfiguration<T> conf)
    {
        super(conf);
    }

    /**
     * Trains a model for the classifier on the given training data.
     * 
     * @param trainingDataIncrement
     *            The current batch of training data.
     * @return Either trained model or a file, stream or something similar to read the model from.
     */
    public abstract Object train(List<List<AnnotationObject>> trainingDataIncrement);

    /**
     * Serialize the trained model.
     * 
     * @return true, if the model was successfully persisted.
     */
    public abstract boolean saveModel();

    /**
     * Deserialize the trained model (from a file, etc.).
     * 
     * @return The deserialized, trained model.
     */
    public abstract Object loadModel();
}
