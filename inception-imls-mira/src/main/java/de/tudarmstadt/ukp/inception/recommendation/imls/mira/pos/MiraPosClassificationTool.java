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
package de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.pos.PosAnnotationObjectLoader;

/**
 * Implementation of POS-Tagging using the Margin Infused Relaxed Algorithm (MIRA).
 */
public class MiraPosClassificationTool
    extends ClassificationTool<MiraConfigurationParameters>
{
    public MiraPosClassificationTool()
    {
        super(-1, MiraPosClassificationTool.class.getName(),
                new MiraPosTrainer(new BaseConfiguration()),
                new MiraPosClassifier(new BaseConfiguration()),
                new PosAnnotationObjectLoader(), true, true);
    }
    
    public MiraPosClassificationTool(long recommenderId, String feature, AnnotationLayer aLayer)
    {
        super(recommenderId, MiraPosClassificationTool.class.getName(),
                new MiraPosTrainer(new BaseConfiguration(feature, recommenderId)),
                new MiraPosClassifier(new BaseConfiguration(feature, recommenderId)),
                new PosAnnotationObjectLoader(aLayer, feature), true, true);
    }
}
