/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@ExtendWith(SpringExtension.class)
public class NoMultipleIncomingRelationsCheckTest
{
    @Configuration
    @Import({ AnnotationSchemaService.class, NoMultipleIncomingRelationsCheck.class })
    static class Config
    {
    }

    @MockBean
    private AnnotationSchemaService annotationService;

    @Autowired
    NoMultipleIncomingRelationsCheck sut;

    @Test
    public void testFail() throws Exception
    {
        AnnotationLayer relationLayer = new AnnotationLayer();
        relationLayer.setName(Dependency.class.getName());

        relationLayer.setType(WebAnnoConst.RELATION_TYPE);
        when(annotationService.listAnnotationLayer(Mockito.isNull()))
                .thenReturn(Arrays.asList(relationLayer));

        JCas jcas = JCasFactory.createJCas();

        jcas.setDocumentText("This is a test.");

        Token spanThis = new Token(jcas, 0, 4);
        spanThis.addToIndexes();

        Token spanIs = new Token(jcas, 5, 7);
        spanIs.addToIndexes();

        Token spanA = new Token(jcas, 8, 9);
        spanA.addToIndexes();

        Dependency dep1 = new Dependency(jcas, 0, 7);
        dep1.setGovernor(spanThis);
        dep1.setDependent(spanIs);
        dep1.addToIndexes();

        Dependency dep2 = new Dependency(jcas, 0, 9);
        dep2.setGovernor(spanA);
        dep2.setDependent(spanIs);
        dep2.addToIndexes();

        List<LogMessage> messages = new ArrayList<>();

        boolean result = sut.check(null, jcas.getCas(), messages);

        messages.forEach(System.out::println);

        assertTrue(result);

        // also check the message itself
        assertEquals(1, messages.size());
        assertEquals(
                "[NoMultipleIncomingRelationsCheck] Relation [This] -> [is] points to span that already has an incoming relation [a] -> [is].",
                messages.get(0).toString());

    }

    @Test
    public void testOK() throws Exception
    {
        AnnotationLayer relationLayer = new AnnotationLayer();
        relationLayer.setName(Dependency.class.getName());

        relationLayer.setType(WebAnnoConst.RELATION_TYPE);
        Mockito.when(annotationService.listAnnotationLayer(Mockito.isNull()))
                .thenReturn(Arrays.asList(relationLayer));

        JCas jcas = JCasFactory.createJCas();

        jcas.setDocumentText("This is a test.");

        Token spanThis = new Token(jcas, 0, 4);
        spanThis.addToIndexes();

        Token spanIs = new Token(jcas, 6, 8);
        spanIs.addToIndexes();

        Token spanA = new Token(jcas, 9, 10);
        spanA.addToIndexes();

        Dependency dep1 = new Dependency(jcas, 0, 8);
        dep1.setGovernor(spanThis);
        dep1.setDependent(spanIs);
        dep1.addToIndexes();

        Dependency dep2 = new Dependency(jcas, 6, 10);
        dep2.setGovernor(spanIs);
        dep2.setDependent(spanA);
        dep2.addToIndexes();

        List<LogMessage> messages = new ArrayList<>();

        boolean result = sut.check(null, jcas.getCas(), messages);

        messages.forEach(System.out::println);

        assertTrue(result);
    }

    @Test
    public void testOkBecauseCoref() throws Exception
    {

        AnnotationLayer relationLayer = new AnnotationLayer();
        relationLayer.setName(CoreferenceChain.class.getName());

        relationLayer.setType(WebAnnoConst.CHAIN_TYPE);
        Mockito.when(annotationService.listAnnotationLayer(Mockito.isNull()))
                .thenReturn(Arrays.asList(relationLayer));

        JCas jcas = JCasFactory.createJCas();

        jcas.setDocumentText("This is a test.");

        Token spanThis = new Token(jcas, 0, 4);
        spanThis.addToIndexes();

        Token spanIs = new Token(jcas, 6, 8);
        spanIs.addToIndexes();

        Token spanA = new Token(jcas, 9, 10);
        spanA.addToIndexes();

        Dependency dep1 = new Dependency(jcas, 0, 8);
        dep1.setGovernor(spanThis);
        dep1.setDependent(spanIs);
        dep1.addToIndexes();

        Dependency dep2 = new Dependency(jcas, 0, 10);
        dep2.setGovernor(spanA);
        dep2.setDependent(spanIs);
        dep2.addToIndexes();

        List<LogMessage> messages = new ArrayList<>();

        boolean result = sut.check(null, jcas.getCas(), messages);

        messages.forEach(System.out::println);

        assertTrue(result);
    }

}
