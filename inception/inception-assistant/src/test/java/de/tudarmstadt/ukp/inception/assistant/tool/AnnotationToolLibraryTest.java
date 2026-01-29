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
package de.tudarmstadt.ukp.inception.assistant.tool;

import static de.tudarmstadt.ukp.inception.assistant.tool.AnnotationToolLibrary.findTextWithStepwiseContext;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.support.uima.Range;

public class AnnotationToolLibraryTest
{
    static List<Arguments> findTextWithStepwiseContextData()
    {
        return asList( //
                arguments("fee foobar fum.", //
                        new MatchSpec("", "foobar", "", ""), //
                        asList(new Range(4, 10))), //
                arguments("fee foobar fum.", //
                        new MatchSpec("", "foo", "", ""), //
                        asList()), //
                arguments("fee foo  bar fum.", //
                        new MatchSpec("", "foo bar", "", ""), //
                        asList(new Range(4, 12))) //
        );
    }

    @MethodSource("findTextWithStepwiseContextData")
    @ParameterizedTest
    void testFindTextWithStepwiseContext(String aText, MatchSpec aMatch,
            List<Range> aExpectedRanges)
    {
        var ranges = findTextWithStepwiseContext(aText, aMatch, 1);
        assertThat(ranges).containsExactlyInAnyOrderElementsOf(aExpectedRanges);
    }
}
