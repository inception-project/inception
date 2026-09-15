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
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorUtils.collectIndexed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

class ClampAnnotationsToDocumentTextRepairTest
{
    ClampAnnotationsToDocumentTextRepair sut;
    JCas jCas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new ClampAnnotationsToDocumentTextRepair();
        // Document text of length 15
        jCas = createText("This is a test.", "en");
    }

    @Test
    void thatAnnotationsBeyondTheDocumentTextAreClamped()
    {
        var annotations = asList( //
                new Token(jCas, 0, 4), // within the text - must not change
                new Token(jCas, 10, 15), // ends exactly at the end - must not change
                new Token(jCas, 10, 16), // the #6246 case - must be clamped
                new Token(jCas, 20, 30)); // entirely outside - collapses to an empty span
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        sut.repair(null, null, jCas.getCas(), messages);

        assertThat(jCas.select(Token.class).asList()) //
                .extracting(Annotation::getBegin, Annotation::getEnd) //
                .containsExactlyInAnyOrder( //
                        tuple(0, 4), //
                        tuple(10, 15), //
                        tuple(10, 15), //
                        tuple(15, 15));

        assertThat(messages).hasSize(2);
    }

    @Test
    void thatClampingDoesNotProduceNegativeSizedAnnotations()
    {
        // A negative-size annotation that is also out of bounds must not come out of the repair
        // with begin > end - that would just trade one broken state for another.
        var ann = new Token(jCas, 20, 5);
        ann.addToIndexes();

        sut.repair(null, null, jCas.getCas(), new ArrayList<>());

        assertThat(ann.getBegin()).isLessThanOrEqualTo(ann.getEnd());
        assertThatNoException().isThrownBy(() -> ann.getCoveredText());
    }

    @Test
    void thatUnindexedButReachableAnnotationsAreAlsoClamped() throws Exception
    {
        // RemoveBomRepair - named as a cause of this corruption - walks reachable feature
        // structures, so it can leave behind out-of-bounds annotations that are reachable through
        // an indexed annotation but not indexed themselves.
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var refTypeDesc = tsd.addType("RefType", null, CAS.TYPE_NAME_ANNOTATION);
        refTypeDesc.addFeature("ref", null, CAS.TYPE_NAME_ANNOTATION);

        var cas = CasCreationUtils.createCas(tsd, null, null);
        cas.setDocumentText("This is a test.");
        var refType = cas.getTypeSystem().getType("RefType");

        // Out of bounds and NOT indexed, but reachable through the indexed annotation below
        var unindexed = cas.createAnnotation(cas.getAnnotationType(), 10, 16);

        var holder = cas.createAnnotation(refType, 0, 1);
        holder.setFeatureValue(refType.getFeatureByBaseName("ref"), unindexed);
        cas.addFsToIndexes(holder);

        sut.repair(null, null, cas, new ArrayList<>());

        assertThat(unindexed.getEnd()) //
                .as("unindexed but reachable annotations must be clamped as well") //
                .isEqualTo(15);
        assertThat(collectIndexed(cas)) //
                .as("the repair must not index a previously unindexed annotation") //
                .doesNotContain(unindexed);
    }

    @Test
    void thatClampedAnnotationsCanBeRenderedAfterwards()
    {
        var ann = new Token(jCas, 10, 16);
        ann.addToIndexes();

        sut.repair(null, null, jCas.getCas(), new ArrayList<>());

        // Before the repair this throws
        // StringIndexOutOfBoundsException: Range [10, 16) out of bounds for length 15
        assertThatNoException().isThrownBy(() -> ann.getCoveredText());
        assertThat(ann.getCoveredText()).isEqualTo("test.");
    }
}
