/*
 * Copyright 2017
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

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.inception.recommendation.imls.conf.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classifier.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.trainer.Trainer;

/**
 * The core class of the incremental machine learning suite. This class combines the parts needed
 * for an evaluation of a machine learning algorithm. It contains references to the following
 * objects:
 * <ul>
 * <li>{@link AnnotationObjectLoader}
 * <li>{@link Trainer}
 * <li>{@link Classifier}
 * </ul>
 * 
 *
 *
 * @param <C>
 *            The configuration parameter class type, used in the configuration of the trainer and
 *            the classifier.
 */
public class ClassificationTool<C>
{
    private long id;
    private String name;
    private Trainer<C> trainer;
    private Classifier<C> classifier;
    private AnnotationObjectLoader loader;
    private boolean trainOnCompleteSentences;

    protected ClassificationTool()
    {
    }

    /**
     * Constructs a new ClassificationTool
     * 
     * @param id
     *            A id for the classification tool. It is recommended to use a unique id.
     * @param name
     *            A name for the classification tool. It is recommended to use a unique name.
     * @param trainer
     *            The trainer class training the model of the implementing machine learning
     *            algorithm.
     * @param classifier
     *            The classifier, i.e. the machine learning algorithm.
     * @param loader
     *            A loader class, used to load annotated data out of a {@link JCas}
     * @param trainOnCompleteSentences
     *            true, if sentences have to be fully annotated, i.e. every token needs an
     *            annotation label != null, to be used for training.
     */
    protected ClassificationTool(long id, String name, Trainer<C> trainer, Classifier<C> classifier,
            AnnotationObjectLoader loader, boolean trainOnCompleteSentences)
    {
        super();
        this.id = id;
        this.name = name;
        this.trainer = trainer;
        this.classifier = classifier;
        this.loader = loader;
        this.trainOnCompleteSentences = trainOnCompleteSentences;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getId()
    {
        return id;
    }

    public Trainer<?> getTrainer()
    {
        return trainer;
    }

    public Classifier<?> getClassifier()
    {
        return classifier;
    }

    public AnnotationObjectLoader getLoader()
    {
        return loader;
    }

    public String getFeature()
    {
        return classifier.getClassifierConfiguration().getFeature();
    }

    public boolean isTrainOnCompleteSentences()
    {
        return trainOnCompleteSentences;
    }

    protected void setTrainer(Trainer<C> trainer)
    {
        this.trainer = trainer;
    }

    protected void setClassifier(Classifier<C> classifier)
    {
        this.classifier = classifier;
    }

    protected void setLoader(AnnotationObjectLoader loader)
    {
        this.loader = loader;
    }

    protected void setTrainOnCompleteSentences(boolean trainOnCompleteSentences)
    {
        this.trainOnCompleteSentences = trainOnCompleteSentences;
    }

    /**
     * Sets the given classifier configuration for the trainer and the classifier at once.
     * 
     * @param conf
     *            A configuration used to set up trainer and classifier properly.
     */
    public void setClassifierConfiguration(ClassifierConfiguration<C> conf)
    {
        if (trainer != null) {
            trainer.setClassifierConfiguration(conf);
        }
        if (classifier != null) {
            classifier.setClassifierConfiguration(conf);
        }
    }
}
