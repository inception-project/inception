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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation;

import junit.framework.TestCase;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.factory.JCasBuilder;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class TestCasDiff extends TestCase {

	/* TODO correct test case
    @Test
    public void testDoDiff() throws Exception {
    	// generate two cases
    	JCas jCas1 = JCasFactory.createJCas();
    	JCasBuilder builder1 = new JCasBuilder(jCas1);
    	getCas1(builder1, jCas1);
    	JCas jCas2 = JCasFactory.createJCas();
    	JCasBuilder builder2 = new JCasBuilder(jCas2);
    	getCas2(builder2, jCas2);
    	Map<String, CAS> diffMap = new HashMap<String, CAS>();
    	diffMap.put("user1", jCas1.getCas());
    	diffMap.put("user2", jCas2.getCas());

    	Type entryType = CasUtil.getType(jCas1.getCas(), Token.class);
    	System.out.println(System.currentTimeMillis());
    	Map<String, Set<FeatureStructure>> result = CasDiff.doDiff(entryType, diffMap, 0, 0);

    	// check result
 //   	assertEquals(result.get("user1").size(), 2);
    	for (FeatureStructure fs : result.get("user1")) {
    		assertTrue(hasPos(fs, "I"));
    	}
    //	assertEquals(result.get("user2").size(), 2);
    	for (FeatureStructure fs : result.get("user2")) {
    		assertTrue(hasPos(fs, "O"));
    	}

    }
    */

    @SuppressWarnings("unused")
    private boolean hasPos(FeatureStructure fs, String posValue) {
		if (fs instanceof POS) {
			POS pos = (POS) fs;
			if(pos.getPosValue().equals(posValue)) {
				return true;
			}
		} else if (fs instanceof Token) {
			Token token = (Token) fs;
			if(token.getPos().getPosValue().equals(posValue)) {
				return true;
			}
		}
		return false;
    }

    public void testDoDiff2() throws Exception {
    	// TODO 1 Token mehr
    }

    @SuppressWarnings("unused")
    private void getCas1(JCasBuilder aBuilder, JCas aJCas) {
    	Token token1 = aBuilder.add("Hallo",Token.class);
    	POS pos1 = new POS(aJCas, token1.getBegin(), token1.getEnd());
    	pos1.setPosValue("I");
    	pos1.addToIndexes();
    	token1.setPos(pos1);
    	Token token2 = aBuilder.add("Welt",Token.class);
    	POS pos2 = new POS(aJCas, token2.getBegin(), token2.getEnd());
    	pos2.setPosValue("N");
    	pos2.addToIndexes();
    	token2.setPos(pos2);
    	Token token3 = aBuilder.add("!",Token.class);
    	POS pos3 = new POS(aJCas, token3.getBegin(), token3.getEnd());
    	pos3.setPosValue("SENT");
    	token3.setPos(pos3);
    	pos3.addToIndexes();
    }

    @SuppressWarnings("unused")
    private void getCas2(JCasBuilder aBuilder, JCas aJCas) {
    	Token token1 = aBuilder.add("Hallo",Token.class);
    	POS pos1 = new POS(aJCas, token1.getBegin(), token1.getEnd());
    	pos1.setPosValue("O");
    	pos1.addToIndexes();
    	token1.setPos(pos1);
    	Token token2 = aBuilder.add("Welt",Token.class);
    	POS pos2 = new POS(aJCas, token2.getBegin(), token2.getEnd());
    	pos2.setPosValue("N");
    	pos2.addToIndexes();
    	token2.setPos(pos2);
    	Token token3 = aBuilder.add("!",Token.class);
    	POS pos3 = new POS(aJCas, token3.getBegin(), token3.getEnd());
    	pos3.setPosValue("SENT");
    	token3.setPos(pos3);
    	pos3.addToIndexes();
    }

}
