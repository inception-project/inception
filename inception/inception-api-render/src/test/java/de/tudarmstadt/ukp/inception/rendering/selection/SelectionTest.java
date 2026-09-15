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
package de.tudarmstadt.ukp.inception.rendering.selection;

import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the bounds violation reported in issue #6246, where an annotation reaches beyond the
 * end of the document text (there: an annotation ending at 3107 over a text of length 3106).
 */
class SelectionTest
{
    Selection sut;
    JCas jCas;
    CAS cas;
    int documentLength;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new Selection();
        jCas = createText("This is a test.", "en");
        cas = jCas.getCas();
        documentLength = jCas.getDocumentText().length();
    }

    @Test
    void thatSelectingAnAnnotationExceedingTheDocumentTextClipsIt()
    {
        assertThatNoException()
                .isThrownBy(() -> sut.selectSpan(cas, documentLength - 5, documentLength + 1));

        assertThat(sut.getBegin()).isEqualTo(documentLength - 5);
        assertThat(sut.getEnd()).isEqualTo(documentLength);
        assertThat(sut.getText()).isEqualTo("test.");
    }

    @Test
    void thatSelectingAnAnnotationFullyOutsideTheDocumentTextClipsIt()
    {
        assertThatNoException()
                .isThrownBy(() -> sut.selectSpan(cas, documentLength + 5, documentLength + 15));

        assertThat(sut.getBegin()).isEqualTo(documentLength);
        assertThat(sut.getEnd()).isEqualTo(documentLength);
        assertThat(sut.getText()).isEmpty();
    }

    @Test
    void thatSelectingAnExistingBrokenAnnotationClipsIt()
    {
        var ann = new Annotation(jCas, documentLength - 5, documentLength + 1);
        ann.addToIndexes();

        assertThatNoException().isThrownBy(() -> sut.selectSpan(ann));

        assertThat(sut.getAnnotation().isSet()) //
                .as("the VID must resolve - a broken annotation is still addressable") //
                .isTrue();
        assertThat(sut.getBegin()).isEqualTo(documentLength - 5);
        assertThat(sut.getEnd()).isEqualTo(documentLength);
        assertThat(sut.getText()).isEqualTo("test.");
    }

    @Test
    void thatSelectingByVidTakesTheOffsetsFromTheAnnotation()
    {
        var ann = new Annotation(jCas, 0, 4);
        ann.addToIndexes();

        assertThatNoException().isThrownBy(() -> sut.selectSpan(VID.of(ann), cas));

        assertThat(sut.getAnnotation().isSet()).isTrue();
        assertThat(sut.getBegin()).isZero();
        assertThat(sut.getEnd()).isEqualTo(4);
        assertThat(sut.getText()).isEqualTo("This");
    }

    @Test
    void thatSelectingByVidClipsABrokenAnnotation()
    {
        var ann = new Annotation(jCas, documentLength - 5, documentLength + 1);
        ann.addToIndexes();

        assertThatNoException().isThrownBy(() -> sut.selectSpan(VID.of(ann), cas));

        assertThat(sut.getBegin()).isEqualTo(documentLength - 5);
        assertThat(sut.getEnd()).isEqualTo(documentLength);
        assertThat(sut.getText()).isEqualTo("test.");
    }

    @Test
    void thatSelectingByAnUnresolvableVidClearsTheSelection()
    {
        assertThatNoException().isThrownBy(() -> sut.selectSpan(new VID(999999), cas));

        assertThat(sut.getAnnotation().isSet()) //
                .as("an unresolvable VID must leave the selection cleared") //
                .isFalse();
    }

    @Test
    void thatSelectingAnAnnotationWithinTheDocumentTextIsUnchanged()
    {
        assertThatNoException().isThrownBy(() -> sut.selectSpan(cas, 0, 4));

        assertThat(sut.getBegin()).isZero();
        assertThat(sut.getEnd()).isEqualTo(4);
        assertThat(sut.getText()).isEqualTo("This");
    }
}
