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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.ner;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.ner.NerAnnotationObjectLoader;

/**
 * The implementation of Named Entity Recognition using simple String matching.
 */
public class StringMatchingNerClassificationTool
    extends ClassificationTool<Object>
{
    public StringMatchingNerClassificationTool()
    {
        super(-1, StringMatchingNerClassificationTool.class.getName(),
            new StringMatchingNerTrainer(new ClassifierConfiguration<>()),
            new StringMatchingNerClassifier(new ClassifierConfiguration<>()),
            new NerAnnotationObjectLoader(), false);
    }

    public StringMatchingNerClassificationTool(long recommenderId, String feature,
        AnnotationLayer aLayer)
    {
        super(recommenderId, StringMatchingNerClassificationTool.class.getName(),
            new StringMatchingNerTrainer(new ClassifierConfiguration<>(feature, recommenderId)),
            new StringMatchingNerClassifier(new ClassifierConfiguration<>(feature, recommenderId)),
            new NerAnnotationObjectLoader(aLayer, feature), false);
    }

}
