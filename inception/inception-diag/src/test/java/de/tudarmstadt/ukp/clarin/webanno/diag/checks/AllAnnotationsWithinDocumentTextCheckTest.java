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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

class AllAnnotationsWithinDocumentTextCheckTest
{
    AllAnnotationsWithinDocumentTextCheck sut;
    SourceDocument document;
    String dataOwner;
    JCas jCas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new AllAnnotationsWithinDocumentTextCheck();
        document = SourceDocument.builder().build();
        // Document text of length 15
        jCas = createText("This is a test.", "en");
    }

    @Test
    void thatAnnotationsWithinTheDocumentTextAreAccepted()
    {
        var annotations = asList( //
                new Annotation(jCas, 0, 4), //
                new Annotation(jCas, 10, 15), // ends exactly at the end of the text
                new Annotation(jCas, 15, 15)); // empty span at the end of the text
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, jCas.getCas(), messages);

        assertThat(result).isTrue();
        assertThat(messages).isEmpty();
    }

    @Test
    void thatUnindexedButReachableAnnotationsBeyondTheDocumentTextAreReported() throws Exception
    {
        // RemoveBomRepair walks reachable feature structures, so out-of-bounds annotations can be
        // left behind that are reachable through an indexed annotation but not indexed themselves.
        // Those break rendering just the same and must be reported.
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var refTypeDesc = tsd.addType("RefType", null, CAS.TYPE_NAME_ANNOTATION);
        refTypeDesc.addFeature("ref", null, CAS.TYPE_NAME_ANNOTATION);

        var cas = CasCreationUtils.createCas(tsd, null, null);
        cas.setDocumentText("This is a test.");
        var refType = cas.getTypeSystem().getType("RefType");

        var unindexed = cas.createAnnotation(cas.getAnnotationType(), 10, 16);
        var holder = cas.createAnnotation(refType, 0, 1);
        holder.setFeatureValue(refType.getFeatureByBaseName("ref"), unindexed);
        cas.addFsToIndexes(holder);

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, cas, messages);

        assertThat(result).isFalse();
        assertThat(messages).hasSize(1);
    }

    @Test
    void thatAnnotationsBeyondTheDocumentTextAreReported()
    {
        // This is the #6246 situation - the annotation ends one character beyond the text
        var annotations = asList( //
                new Annotation(jCas, 0, 4), //
                new Annotation(jCas, 10, 16));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, jCas.getCas(), messages);

        assertThat(result).isFalse();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()) //
                .contains("[uima.tcas.Annotation] at [10-16] extends beyond the end of the "
                        + "document text [15]");
    }
}
