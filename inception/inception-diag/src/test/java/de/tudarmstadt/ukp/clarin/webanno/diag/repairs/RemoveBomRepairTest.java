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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.random.RandomGenerator;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

class RemoveBomRepairTest
{
    private RandomGenerator rng = RandomGenerator.getDefault();

    private RemoveBomRepair sut = new RemoveBomRepair();

    @Test
    void test() throws Exception
    {
        var annotationCount = 100;
        var cas = CasFactory.createText("\uFEFF" + generateRandomString(100));

        for (int n = 0; n < annotationCount; n++) {
            var begin = rng.nextInt(0, cas.getDocumentText().length());
            var end = rng.nextInt(0, cas.getDocumentText().length());
            var ann = cas.createAnnotation(cas.getAnnotationType(), min(begin, end),
                    max(begin, end));
            cas.addFsToIndexes(ann);
        }

        var coveredTextsBefore = new HashMap<AnnotationFS, String>();
        var annotations = cas.select(Annotation.class).asList();
        for (var ann : annotations) {
            coveredTextsBefore.put(ann, ann.getCoveredText());
        }

        var messages = new ArrayList<LogMessage>();
        sut.repair(null, null, cas, messages);

        assertThat(annotations).hasSizeGreaterThan(annotationCount);

        for (var ann : annotations) {
            var oldText = coveredTextsBefore.get(ann);
            if (oldText.length() > 0
                    && (oldText.charAt(0) == '\uFEFF' || oldText.charAt(0) == '\uFFFE')) {
                oldText = oldText.substring(1);
            }
            assertThat(ann.getCoveredText()).isEqualTo(oldText);
        }
    }

    private String generateRandomString(int len)
    {
        var chars = new char[len];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) rng.nextInt('0', 'z');
        }
        return String.valueOf(chars);
    }
}
