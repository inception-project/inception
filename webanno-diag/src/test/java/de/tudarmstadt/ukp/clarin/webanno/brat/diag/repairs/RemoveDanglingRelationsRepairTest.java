/*
 * Copyright 2015
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
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.diag.repairs;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.AllFeatureStructuresIndexedCheck;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.RemoveDanglingRelationsRepair;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class RemoveDanglingRelationsRepairTest
{
    @Test
    public void test()
        throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        jcas.setDocumentText("This is a test.");
        
        Token span1 = new Token(jcas, 0, 4);
        span1.addToIndexes();
        
        Token span2 = new Token(jcas, 6, 8);
        
        Dependency dep = new Dependency(jcas, 0, 8);
        dep.setGovernor(span1);
        dep.setDependent(span2);
        dep.addToIndexes();
        
        List<LogMessage> messages = new ArrayList<>();
        CasDoctor cd = new CasDoctor(RemoveDanglingRelationsRepair.class,
                AllFeatureStructuresIndexedCheck.class);
        // A project is not required for this check
        boolean result = cd.analyze(null, jcas.getCas(), messages);
        // A project is not required for this repair
        cd.repair(null, jcas.getCas(), messages);
        
        assertFalse(result);
        
        messages.forEach(System.out::println);
    }
}
