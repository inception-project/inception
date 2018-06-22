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
package de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.pos;

import java.util.List;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.inception.recommendation.api.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.util.CasUtil;

public class PosAnnotationObjectLoader implements AnnotationObjectLoader
{
    private String feature = "PosValue";
    private AnnotationLayer layer;

    public PosAnnotationObjectLoader()
    {

    }

    public PosAnnotationObjectLoader(AnnotationLayer aLayer, String aFeature)
    {
        this.feature = aFeature;
        this.layer = aLayer;
    }

    @Deprecated
    @Override
    public List<List<AnnotationObject>> loadAnnotationObjectsForTesting(JCas jCas)
    {
        return CasUtil.loadAnnotatedSentences(jCas, POS.class, "PosValue", POS::getPosValue);
    }

    @Override
    public List<List<AnnotationObject>> loadAnnotationObjects(JCas jCas, long aRecommenderId)
    {
        return CasUtil.loadAnnotatedSentences(jCas, aRecommenderId, layer, feature);
    }
}
