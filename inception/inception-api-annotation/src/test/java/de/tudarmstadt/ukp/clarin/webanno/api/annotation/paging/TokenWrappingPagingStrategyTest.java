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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;

public class TokenWrappingPagingStrategyTest
{
    @Test
    public void thatMultipleConsecutiveLineBreaksWork() throws Exception
    {
        TokenWrappingPagingStrategy sut = new TokenWrappingPagingStrategy(120);

        JCas jcas = JCasFactory.createJCas();
        JCasBuilder builder = new JCasBuilder(jcas);
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
        TokenWrappingPagingStrategy sut = new TokenWrappingPagingStrategy(10);

        JCas jcas = JCasFactory.createJCas();
        JCasBuilder builder = new JCasBuilder(jcas);
        builder.add(StringUtils.repeat("a", 20), Token.class);
        builder.add("\n");
        builder.add(StringUtils.repeat("b", 15), Token.class);
        builder.add("\n");
        builder.add(StringUtils.repeat("c", 11), Token.class);
        builder.add("\n");
        builder.add(StringUtils.repeat("d", 10), Token.class);
        builder.add("\n");
        builder.add(StringUtils.repeat("e", 9), Token.class);
        builder.add("\n");
        builder.add(StringUtils.repeat("f", 1), Token.class);
        builder.add("\n");
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
                        tuple(70, 71, "f"));
    }

    @Test
    public void thatWrappingWork() throws Exception
    {
        TokenWrappingPagingStrategy sut = new TokenWrappingPagingStrategy(11);

        JCas jcas = JCasFactory.createJCas();
        JCasBuilder builder = new JCasBuilder(jcas);
        for (int n = 0; n < 10; n++) {
            builder.add(StringUtils.repeat("a", 3), Token.class);
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
}
