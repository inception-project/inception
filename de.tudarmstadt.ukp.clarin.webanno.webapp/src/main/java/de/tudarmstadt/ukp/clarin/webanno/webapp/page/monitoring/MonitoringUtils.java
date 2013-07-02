/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import static org.uimafit.util.JCasUtil.select;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class MonitoringUtils
{
    /**
     * Returns expected number of annotation
     * We assume one named entity, coreftype, and corefchain annotation per sentence
     * and one pos and dependency annotation per token.
     * @param aJCas
     * @return
     */
 public static int expectedAnnotations(JCas aJCas){
     int namedEntity = select(aJCas, Sentence.class).size();
     int pos = select(aJCas, Token.class).size();
     int dep = select(aJCas, Token.class).size();
     int coreftype =  select(aJCas, Sentence.class).size();
     int corefchain =  select(aJCas, Sentence.class).size();
     return namedEntity+ pos+dep+coreftype+corefchain;
 }

 /**
  * returns the actual annotations found in the CAS object
  * @param aJCas
  * @return
  */
 public static int actualAnnotations(JCas aJCas){
     int namedEntity = select(aJCas, NamedEntity.class).size();
     int pos = select(aJCas, POS.class).size();
     int dep = select(aJCas, Dependency.class).size();
     int coreftype = select(aJCas, CoreferenceLink.class).size();
     int corefchain = select(aJCas, CoreferenceChain.class).size();
     return namedEntity+ pos+dep+coreftype+corefchain;
 }
}
