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
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;

public class ExternalClassificationTool
    extends ClassificationTool<Object>

{
    public ExternalClassificationTool(long recommenderId, String feature, String type,
        ExternalClassifierTraits traits)
    {
        super(recommenderId, ExternalClassificationTool.class.getName(),
            new ExternalTrainer(new BaseConfiguration()),
            new ExternalClassifier(new BaseConfiguration(feature),
                new CustomAnnotationObjectLoader(feature, type), traits, recommenderId),
            new CustomAnnotationObjectLoader(feature, type), true, false);
    }

}
