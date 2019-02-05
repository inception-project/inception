/*
 * Copyright 2019
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

package de.tudarmstadt.ukp.inception.externalsearch.cluster;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;


public class UnitByQueryWordAnnotator
        extends JCasAnnotator_ImplBase
{
    public static String TYPE_NAME_UNIT = "Unit";
    public static final String PARAM_QUERY_WORD = "queryWord";
    @ConfigurationParameter(name = PARAM_QUERY_WORD, mandatory = true)
    private String queryWord;
    
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        Type tUnit = aJCas.getTypeSystem().getType(TYPE_NAME_UNIT);
        for (Sentence sentence : JCasUtil.select(aJCas, Sentence.class)) {
            if (sentence.getCoveredText().contains(queryWord)) {
                AnnotationFS unit = aJCas.getCas().createAnnotation(tUnit,
                        sentence.getBegin(), sentence.getEnd());
                aJCas.getCas().addFsToIndexes(unit);
            }
        }
    }
}
