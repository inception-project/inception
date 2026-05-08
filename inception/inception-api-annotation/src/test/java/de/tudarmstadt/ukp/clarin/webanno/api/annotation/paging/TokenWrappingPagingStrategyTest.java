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
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy.LINE_SEPARATORS;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;

public class TokenWrappingPagingStrategyTest
{
    private JCas jcas;

    @BeforeEach
    void setup() throws Exception
    {
        jcas = JCasFactory.createJCas();
    }

    @Test
    public void thatMultipleConsecutiveLineBreaksWork() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(120);

        var builder = new JCasBuilder(jcas);
        builder.add("See-", Token.class);
        builder.add("\n");
        builder.add("\n");
        builder.add("\n");
        builder.add("Power", Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "See-", //
                        "", //
                        "", //
                        "Power");
    }

    @Test
    public void thatLinesOfDifferntLenghtsWork() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add(repeat("a", 20), Token.class);
        builder.add("\n");
        builder.add(repeat("b", 15), Token.class);
        builder.add("\n");
        builder.add(repeat("c", 11), Token.class);
        builder.add("\n");
        builder.add(repeat("d", 10), Token.class);
        builder.add("\n");
        builder.add(repeat("e", 9), Token.class);
        builder.add("\n");
        builder.add(repeat("f", 1), Token.class);
        builder.add("\n");
        builder.add(repeat("g", 1), Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas())) //
                .extracting( //
                        Unit::getBegin, //
                        Unit::getEnd, //
                        u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd())) //
                .containsExactly( //
                        tuple(0, 20, "aaaaaaaaaaaaaaaaaaaa"), //
                        tuple(21, 36, "bbbbbbbbbbbbbbb"), //
                        tuple(37, 48, "ccccccccccc"), //
                        tuple(49, 59, "dddddddddd"), //
                        tuple(60, 69, "eeeeeeeee"), //
                        tuple(70, 71, "f"), //
                        tuple(72, 73, "g"));
    }

    @Test
    public void thatWrappingWork() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(11);

        var builder = new JCasBuilder(jcas);
        for (int n = 0; n < 10; n++) {
            builder.add(repeat("a", 3), Token.class);
            builder.add(" ");
        }
        builder.close();

        assertThat(sut.units(jcas.getCas())) //
                .extracting( //
                        Unit::getBegin, //
                        Unit::getEnd, //
                        u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd())) //
                .containsExactly( //
                        tuple(0, 11, "aaa aaa aaa"), //
                        tuple(12, 23, "aaa aaa aaa"), //
                        tuple(24, 35, "aaa aaa aaa"), //
                        tuple(36, 39, "aaa"));
    }

    @Test
    void testMixedLineBreaks() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        var i = 1;
        for (var sep : LINE_SEPARATORS) {
            builder.add(repeat("a", 3) + i, Token.class);
            builder.add(" ");
            builder.add(repeat("b", 3) + i, Token.class);
            builder.add(" ");
            builder.add(repeat("c", 3) + i, Token.class);
            builder.add(sep);
            i++;
        }
        builder.add("end", Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "aaa1 bbb1", "ccc1", //
                        "aaa2 bbb2", "ccc2", //
                        "aaa3 bbb3", "ccc3", //
                        "aaa4 bbb4", "ccc4", //
                        "aaa5 bbb5", "ccc5", //
                        "aaa6 bbb6", "ccc6", //
                        "end");
    }

    @Test
    void testEndingWithNewline() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add(repeat("a", 3), Token.class);
        builder.add(CR);
        builder.add(CR);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        "aaa");
    }

    @Test
    void testEmptyDocument() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        jcas.setDocumentText("");

        assertThat(sut.units(jcas.getCas())).isEmpty();
    }

    @Test
    void testNoTokens() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        jcas.setDocumentText("Some text without tokens");

        assertThat(sut.units(jcas.getCas())).isEmpty();
    }

    @Test
    void testSingleToken() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add("token", Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("token");
    }

    @Test
    void testTokenLongerThanMaxLineLength() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add(repeat("a", 25), Token.class);
        builder.add(" ");
        builder.add("b", Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly( //
                        repeat("a", 25), //
                        "b");
    }

    @Test
    void testUnitsIgnoresIndexParameters() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add("aaa", Token.class);
        builder.add(" ");
        builder.add("bbb", Token.class);
        builder.add(" ");
        builder.add("ccc", Token.class);
        builder.add("\n");
        builder.add("ddd", Token.class);
        builder.add(" ");
        builder.add("eee", Token.class);
        builder.close();

        var allUnits = sut.units(jcas.getCas());
        var rangeUnits = sut.units(jcas.getCas(), 2, 3);

        // TokenWrappingPagingStrategy doesn't filter by index parameters
        assertThat(rangeUnits).isEqualTo(allUnits);
    }

    @Test
    void testUnitCount() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add("aaa", Token.class);
        builder.add(" ");
        builder.add("bbb", Token.class);
        builder.add("\n");
        builder.add("ccc", Token.class);
        builder.close();

        var count = sut.unitCount(jcas.getCas());
        var units = sut.units(jcas.getCas());

        assertThat(count).isEqualTo(units.size());
    }

    @Test
    void testTokensWithGaps() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(20);

        jcas.setDocumentText("aaa   bbb   ccc");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 6, 9).addToIndexes();
        new Token(jcas, 12, 15).addToIndexes();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("aaa   bbb   ccc");
    }

    @Test
    void testUnitIndexesStartAtOne() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(10);

        var builder = new JCasBuilder(jcas);
        builder.add("aaa", Token.class);
        builder.add("\n");
        builder.add("bbb", Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas())).extracting(Unit::getIndex).allMatch(idx -> idx >= 1);
    }

    @Test
    void testMultipleTokensOnSameLine() throws Exception
    {
        var sut = new TokenWrappingPagingStrategy(20);

        var builder = new JCasBuilder(jcas);
        builder.add("word1", Token.class);
        builder.add(" ");
        builder.add("word2", Token.class);
        builder.add(" ");
        builder.add("word3", Token.class);
        builder.close();

        assertThat(sut.units(jcas.getCas()))
                .extracting(u -> jcas.getDocumentText().substring(u.getBegin(), u.getEnd()))
                .containsExactly("word1 word2 word3");
    }
}
