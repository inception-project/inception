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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;

class PerParagraphContextGeneratorTest
{

    @Test
    void test() throws Exception
    {
        var cas = CasFactory.createCas();
        cas.setDocumentText("""


                This is paragraph 1.

                This is paragraph 2 sentence 1.\r
                This is paragraph 2 sentence 2.


                This is paragraph 3 sentence 1.

                This is paragraph 4 sentence 1.
                """);

        var sut = new PerParagraphContextGenerator();
        var result = sut.generate(null, cas, 0, cas.getDocumentText().length(), null).toList();

        assertThat(result) //
                .extracting( //
                        c -> c.getRange().getBegin(), //
                        c -> c.getRange().getEnd(), //
                        PromptContext::getText) //
                .containsExactly( //
                        tuple(2, 24, "This is paragraph 1."), //
                        tuple(24, 91,
                                "This is paragraph 2 sentence 1.\r\nThis is paragraph 2 sentence 2."), //
                        tuple(91, 124, "This is paragraph 3 sentence 1."), //
                        tuple(124, 156, "This is paragraph 4 sentence 1."));
    }

}
