/*
 * Copyright 2016
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
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static de.tudarmstadt.ukp.dkpro.core.testing.IOTestRunner.testOneWay;
import static de.tudarmstadt.ukp.dkpro.core.testing.IOTestRunner.testRoundTrip;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class ConllUReaderWriterTest
{
    @Test
    public void roundTrip()
        throws Exception
    {
        testRoundTrip(
                createReaderDescription(ConllUReader.class),
                createEngineDescription(ConllUWriter.class,
                        ConllUWriter.PARAM_WRITE_TEXT_COMMENT, false),
                "conll/u/conllu-en-orig.conll");
    }

    @Ignore("This unfortunately doesn't work yet.")
    @Test
    public void roundTripV2EmptyNodes()
        throws Exception
    {
        testRoundTrip(ConllUReader.class, ConllUWriter.class, "conll/u_v2/conllu-empty_nodes.conll");
    }

    @Test
    public void roundTripV2MorphologicalAnnotation()
        throws Exception
    {
        testRoundTrip(ConllUReader.class, ConllUWriter.class, "conll/u_v2/conllu-morphological_annotation.conll");
    }

    @Ignore("This unfortunately doesn't work yet.")
    @Test
    public void roundTripV2ParagraphAndDocumentBoundaries()
        throws Exception
    {
        testRoundTrip(
                createReaderDescription(ConllUReader.class),
                createEngineDescription(ConllUWriter.class,
                        ConllUWriter.PARAM_WRITE_TEXT_COMMENT, true),
                "conll/u_v2/conllu-paragraph_and_document_boundaries.conll");
    }

    @Test
    public void roundTripV2SentenceBoundariesAndComments()
        throws Exception
    {
        testRoundTrip(
                createReaderDescription(ConllUReader.class),
                createEngineDescription(ConllUWriter.class,
                        ConllUWriter.PARAM_WRITE_TEXT_COMMENT, true),
                "conll/u_v2/conllu-sentence_bounaries_and_comments.conll");
    }

    @Test
    public void roundTripV2SyntacticAnnotation()
        throws Exception
    {
        testRoundTrip(ConllUReader.class, ConllUWriter.class, "conll/u_v2/conllu-syntactic_annotation.conll");
    }

    @Ignore("This unfortunately doesn't work yet.")
    @Test
    public void roundTripV2UntokenizedText()
        throws Exception
    {
        testRoundTrip(
                createReaderDescription(ConllUReader.class),
                createEngineDescription(ConllUWriter.class,
                        ConllUWriter.PARAM_WRITE_TEXT_COMMENT, true),
                "conll/u_v2/conllu-untokenized_text.conll");
    }

    @Test
    public void roundTripV2WordsAndTokens()
        throws Exception
    {
        testRoundTrip(ConllUReader.class, ConllUWriter.class, "conll/u_v2/conllu-words_and_tokens.conll");
    }

    @Test
    public void withComments()
        throws Exception
    {
        testOneWay(
                createReaderDescription(ConllUReader.class),
                createEngineDescription(ConllUWriter.class,
                        ConllUWriter.PARAM_WRITE_TEXT_COMMENT, false),
                "conll/u/conllu-en-ref.conll",
                "conll/u/conllu-en-orig2.conll");
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
