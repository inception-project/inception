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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.ner.NerAnnotationObjectLoader;
import opennlp.tools.util.TrainingParameters;

/**
 * Implementation of Named Entity Recognition using the OpenNlp library.
 */
public class OpenNlpNerClassificationTool
    extends ClassificationTool<TrainingParameters>
{
    public OpenNlpNerClassificationTool()
    {
        super(-1, OpenNlpNerClassificationTool.class.getName(),
            new OpenNlpNerTrainer(new BaseConfiguration()),
            new OpenNlpNerClassifier(new BaseConfiguration()), new NerAnnotationObjectLoader(),
            false, true);
    }

    public OpenNlpNerClassificationTool(long recommenderId, String feature, AnnotationLayer aLayer)
    {
        super(recommenderId, OpenNlpNerClassificationTool.class.getName(),
            new OpenNlpNerTrainer(new BaseConfiguration(feature, recommenderId)),
            new OpenNlpNerClassifier(new BaseConfiguration(feature, recommenderId)),
            new NerAnnotationObjectLoader(aLayer, feature), false, true);
    }
}
