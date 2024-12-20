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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.CR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.CRLF;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.LF;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.LINE_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.NEL;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.PARAGRAPH_SEPARATOR;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

class LineOrientedPagingStrategyTest
{
    @Test
    void testMixedLineBreaks() throws Exception
    {
        var sut = new LineOrientedPagingStrategy();

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText( //
                "Line1" + CR + //
                        "Line2" + LF + //
                        "Line3" + CRLF + //
                        "Line4" + NEL + //
                        "Line5" + LINE_SEPARATOR + //
                        "Line6" + PARAGRAPH_SEPARATOR + //
                        "Line7");

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "Line1", //
                        "Line2", //
                        "Line3", //
                        "Line4", //
                        "Line5", //
                        "Line6", //
                        "Line7");
    }

    @Test
    void testConsecutiveLineBreaks() throws Exception
    {
        var sut = new LineOrientedPagingStrategy();

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText( //
                "Line1" + CR + CR + //
                        "Line2" + LF + LF + //
                        "Line3" + CRLF + CRLF + //
                        "Line4" + NEL + NEL + //
                        "Line5" + LINE_SEPARATOR + LINE_SEPARATOR + //
                        "Line6" + PARAGRAPH_SEPARATOR + PARAGRAPH_SEPARATOR + //
                        "Line7");

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "Line1", "", //
                        "Line2", "", //
                        "Line3", "", //
                        "Line4", "", //
                        "Line5", "", //
                        "Line6", "", //
                        "Line7");
    }

    @Test
    void testEndingWithNewline() throws Exception
    {
        var sut = new LineOrientedPagingStrategy();

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText( //
                "Line1" + CR + CR);

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "Line1", "");
    }
}
