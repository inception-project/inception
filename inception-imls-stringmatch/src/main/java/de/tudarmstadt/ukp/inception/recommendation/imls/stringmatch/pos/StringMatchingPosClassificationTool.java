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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.pos;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.pos.PosAnnotationObjectLoader;

/**
 * The implementation of POS-Tagging using simple String matching.
 */
public class StringMatchingPosClassificationTool
    extends ClassificationTool<Object>
{
    public StringMatchingPosClassificationTool()
    {
        super(-1, StringMatchingPosClassificationTool.class.getName(),
            new StringMatchingPosTrainer(new ClassifierConfiguration<>()),
            new StringMatchingPosClassifier(new ClassifierConfiguration<>()),
            new PosAnnotationObjectLoader(), false, true);
    }

    public StringMatchingPosClassificationTool(long recommenderId, String feature,
        AnnotationLayer aLayer)
    {
        super(recommenderId, StringMatchingPosClassificationTool.class.getName(),
            new StringMatchingPosTrainer(new ClassifierConfiguration<>(feature, recommenderId)),
            new StringMatchingPosClassifier(new ClassifierConfiguration<>(feature, recommenderId)),
            new PosAnnotationObjectLoader(aLayer, feature), false, true);
    }
}
