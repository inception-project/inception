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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.ner.NerAnnotationObjectLoader;

/**
 * The named entity linker loads annotations of the <b>value</b> feature and predicts on the
 * <b>identifier</b> feature of the Named Entity layer.
 **/

public class NamedEntityLinkerClassificationTool
    extends ClassificationTool<Object>
{
    private static final String NAMED_ENTITY_CLASSIFICATION_FEATURE = "value";

    public NamedEntityLinkerClassificationTool(long recommenderId, String feature,
        AnnotationLayer aLayer, KnowledgeBaseService kbService, ConceptLinkingService clService,
        DocumentService docService, AnnotationSchemaService annoService,
        FeatureSupportRegistry fsRegistry)
    {
        super(recommenderId, NamedEntityLinkerClassificationToolFactory.class.getName(),
            new NamedEntityTrainer(new ClassifierConfiguration<>(feature, recommenderId)),
            new NamedEntityLinker(new ClassifierConfiguration<>(feature, recommenderId), kbService,
                clService, docService, annoService, fsRegistry),
            new NerAnnotationObjectLoader(aLayer, NAMED_ENTITY_CLASSIFICATION_FEATURE), false, false);
    }

}
