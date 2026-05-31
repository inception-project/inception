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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;

class SpanPositionTest
{
    /**
     * The position's collection/document identity must be read from the INCEpTION-internal
     * {@link CASMetadata} ({@code projectName} / {@code sourceDocumentName}) - not from the DKPro
     * {@code DocumentMetaData}.
     */
    @Test
    void forAnnotation_readsIdentityFromCasMetadata() throws Exception
    {
        var tsd = CasCreationUtils.mergeTypeSystems(
                List.of(TypeSystemDescriptionFactory.createTypeSystemDescription(),
                        TypeSystemDescriptionFactory.createTypeSystemDescription(
                                "de/tudarmstadt/ukp/clarin/webanno/api/type/webanno-internal")));
        var jcas = JCasFactory.createJCas(tsd);
        jcas.setDocumentText("Hello world");
        var cas = jcas.getCas();

        var casMetadata = cas
                .createAnnotation(cas.getTypeSystem().getType(CASMetadata.class.getName()), 0, 0);
        FSUtil.setFeature(casMetadata, "projectName", "coll1");
        FSUtil.setFeature(casMetadata, "sourceDocumentName", "doc1");
        cas.addFsToIndexes(casMetadata);

        var token = new Token(jcas, 0, 5);
        token.addToIndexes();

        var pos = SpanPosition.builder().forAnnotation(token).build();

        assertThat(pos.getCollectionId()).isEqualTo("coll1");
        assertThat(pos.getDocumentId()).isEqualTo("doc1");
        assertThat(pos.getType()).isEqualTo(Token.class.getName());
    }

    @Test
    void forAnnotation_withoutCasMetadata_leavesIdentityNull() throws Exception
    {
        var tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var jcas = JCasFactory.createJCas(tsd);
        jcas.setDocumentText("Hello world");

        var token = new Token(jcas, 0, 5);
        token.addToIndexes();

        var pos = SpanPosition.builder().forAnnotation(token).build();

        assertThat(pos.getCollectionId()).isNull();
        assertThat(pos.getDocumentId()).isNull();
    }
}
