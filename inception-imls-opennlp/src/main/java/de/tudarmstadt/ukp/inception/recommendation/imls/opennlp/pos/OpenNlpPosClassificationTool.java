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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.pos.PosAnnotationObjectLoader;
import opennlp.tools.util.TrainingParameters;

/**
 * Implementation of POS-Tagging using the OpenNlp library.
 */
public class OpenNlpPosClassificationTool
    extends ClassificationTool<TrainingParameters>
{
    public OpenNlpPosClassificationTool()
    {
        super(-1, OpenNlpPosClassificationTool.class.getName(),
            new OpenNlpPosTrainer(new BaseConfiguration()),
            new OpenNlpPosClassifier(new BaseConfiguration()), new PosAnnotationObjectLoader(),
            true);
    }

    public OpenNlpPosClassificationTool(long recommenderId, int beamSize, String feature,
        AnnotationLayer aLayer)
    {
        super(recommenderId, OpenNlpPosClassificationTool.class.getName(),
            new OpenNlpPosTrainer(new CustomConfiguration(beamSize, feature, recommenderId)),
            new OpenNlpPosClassifier(new CustomConfiguration(beamSize, feature, recommenderId)),
            new PosAnnotationObjectLoader(aLayer, feature), true);
    }
}
