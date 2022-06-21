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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

class SwitchBeginAndEndOnNegativeSizedAnnotationsRepairTest
{
    SwitchBeginAndEndOnNegativeSizedAnnotationsRepair sut;
    Project project;
    JCas jCas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SwitchBeginAndEndOnNegativeSizedAnnotationsRepair();
        project = new Project();
        jCas = JCasFactory.createJCas();
    }

    @Test
    void test()
    {
        var annotations = asList( //
                new Token(jCas, 10, 9), //
                new Token(jCas, 10, 10), //
                new Token(jCas, 10, 11));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        sut.repair(project, jCas.getCas(), messages);

        assertThat(jCas.select(Token.class).asList()) //
                .extracting(Annotation::getBegin, Annotation::getEnd)
                .containsExactly(tuple(9, 10), tuple(10, 11), tuple(10, 10));
    }
}
