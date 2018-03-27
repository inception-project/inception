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
package de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.ner;

import java.util.List;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.CasUtil;

public class NerAnnotationObjectLoader implements AnnotationObjectLoader
{

    @Override
    public List<List<AnnotationObject>> loadAnnotationObjects(JCas jCas, String feature)
    {
        return CasUtil.loadAnnotatedSentences(jCas, NamedEntity.class, feature, ne -> {
            return ne.getValue();
        });
    }
    
    @Override
    public List<List<AnnotationObject>> loadAnnotationObjects(JCas jCas)
    {
        return CasUtil.loadAnnotatedSentences(jCas, NamedEntity.class, "value", ne -> {
            return ne.getValue();
        });
    }
}
