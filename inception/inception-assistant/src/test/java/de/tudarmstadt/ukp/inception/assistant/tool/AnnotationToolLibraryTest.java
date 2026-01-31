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
                // Basic word boundary tests
                arguments("fee foobar fum.", //
                        new MatchSpec("", "foobar", "", ""), //
                        asList(new Range(4, 10))), //
                arguments("fee foobar fum.", //
                        new MatchSpec("", "foo", "", ""), //
                        asList()), // Should not match partial word //

                // Whitespace normalization tests
                arguments("fee foo  bar fum.", //
                        new MatchSpec("", "foo bar", "", ""), //
                        asList(new Range(4, 12))), // Multiple spaces should match
                arguments("fee foo   bar fum.", //
                        new MatchSpec("", "foo bar", "", ""), //
                        asList(new Range(4, 13))), // Three spaces should match
                arguments("fee foo\tbar fum.", //
                        new MatchSpec("", "foo bar", "", ""), //
                        asList(new Range(4, 11))), // Tab should match space (1 char instead of 2)

                // Different whitespace types in search text
                arguments("fee foo bar fum.", //
                        new MatchSpec("", "foo\tbar", "", ""), //
                        asList(new Range(4, 11))), // Tab in search should match space in doc
                arguments("fee foo\tbar fum.", //
                        new MatchSpec("", "foo\tbar", "", ""), //
                        asList(new Range(4, 11))), // Tab in both should match
                arguments("fee foo bar fum.", //
                        new MatchSpec("", "foo\nbar", "", ""), //
                        asList(new Range(4, 11))), // Newline in search should match space in doc
                arguments("fee foo\n\nbar fum.", //
                        new MatchSpec("", "foo bar", "", ""), //
                        asList(new Range(4, 12))), // Multiple newlines in doc should match space in
                                                   // search
                arguments("fee foo  bar fum.", //
                        new MatchSpec("", "foo\t\nbar", "", ""), //
                        asList(new Range(4, 12))), // Mixed whitespace in search should match spaces
                                                   // in doc

                // Multiple matches
                arguments("foo bar foo baz foo.", //
                        new MatchSpec("", "foo", "", ""), //
                        asList(new Range(0, 3), new Range(8, 11), new Range(16, 19))), //
                arguments("test test test.", //
                        new MatchSpec("", "test", "", ""), //
                        asList(new Range(0, 4), new Range(5, 9), new Range(10, 14))), //

                // Word boundaries at start and end of document
                arguments("foo bar.", //
                        new MatchSpec("", "foo", "", ""), //
                        asList(new Range(0, 3))), // Match at start
                arguments("bar foo", //
                        new MatchSpec("", "foo", "", ""), //
                        asList(new Range(4, 7))), // Match at end
                arguments("foo", //
                        new MatchSpec("", "foo", "", ""), //
                        asList(new Range(0, 3))), // Entire document

                // Word boundaries with punctuation
                arguments("Hello, world!", //
                        new MatchSpec("", "world", "", ""), //
                        asList(new Range(7, 12))), //
                arguments("(foo) [bar]", //
                        new MatchSpec("", "foo", "", ""), //
                        asList(new Range(1, 4))), //
                arguments("end.", //
                        new MatchSpec("", "end", "", ""), //
                        asList(new Range(0, 3))), //

                // No matches - word boundary violations
                arguments("foobar", //
                        new MatchSpec("", "foo", "", ""), //
                        asList()), // foo is part of foobar //
                arguments("barfoo", //
                        new MatchSpec("", "foo", "", ""), //
                        asList()), // foo is part of barfoo //
                arguments("foobar barfoo", //
                        new MatchSpec("", "foo", "", ""), //
                        asList()), // foo only appears as partial word //

                // Context disambiguation
                arguments("foo bar foo baz.", //
                        new MatchSpec("", "foo", " bar", ""), //
                        asList(new Range(0, 3))), // Only first foo has " bar" after
                arguments("fee foo fum foo.", //
                        new MatchSpec("fee", "foo", "", ""), //
                        asList(new Range(4, 7))), // "fee" matches with optional boundary whitespace
                arguments("fee foo fum foo.", //
                        new MatchSpec("fee ", "foo", "", ""), //
                        asList(new Range(4, 7))), // Only first foo has "fee " before
                arguments("fee\t foo fum foo.", //
                        new MatchSpec("", "fum", "", ""), //
                        asList(new Range(9, 12))), // Match "fum" in document
                arguments("fee foo fum \t\nfoo.", //
                        new MatchSpec("", "foo", " fum", ""), //
                        asList(new Range(4, 7))), // Only first foo has " fum" after
                arguments("a foo b foo c.", //
                        new MatchSpec("a ", "foo", " b", ""), //
                        asList(new Range(2, 5))), // Only first foo matches both contexts

                // Context with newlines (whitespace normalization in context)
                // Test WITH boundary whitespace in context spec
                arguments("2009. Nine\nmonths later, Obama was named the prize.", //
                        new MatchSpec("Nine\nmonths later, ", "Obama was named the prize", ".", ""), //
                        asList(new Range(25, 50))), // Newline in context with trailing space
                arguments("include the Patient Protection and Affordable Care Act.", //
                        new MatchSpec("include the ", "Patient Protection and Affordable Care Act",
                                ".", ""), //
                        asList(new Range(12, 54))), // Space in context spec
                arguments("text before target after text.", //
                        new MatchSpec("before ", "target", " after", ""), //
                        asList(new Range(12, 18))), // Simple context match with explicit spaces
                arguments("text before\ntarget\nafter text.", //
                        new MatchSpec("before\n", "target", "\nafter", ""), //
                        asList(new Range(12, 18))), // Newlines in context spec

                // Test WITHOUT boundary whitespace in context spec (optional boundary whitespace)
                arguments("2009.\nNine\nmonths\nlater, he won.", //
                        new MatchSpec("Nine\nmonths\nlater", "he won", "", ""), //
                        asList()), // Context missing comma - should not match (punctuation matters)
                arguments("end. First\t\nsentence here.", //
                        new MatchSpec("First\t\nsentence", "here", "", ""), //
                        asList(new Range(21, 25))), // Tab and newline, no trailing space
                arguments("text before target after text.", //
                        new MatchSpec("before", "target", "after", ""), //
                        asList(new Range(12, 18))), // No boundary whitespace in context spec
                arguments("text before\ntarget\nafter text.", //
                        new MatchSpec("before", "target", "after", ""), //
                        asList(new Range(12, 18))), // Newlines in doc, no boundary whitespace in
                                                    // spec
                arguments("a\t\nb  c\nd.", //
                        new MatchSpec("a\t\nb", "c", "d", ""), //
                        asList(new Range(6, 7))), // Mixed whitespace, no boundary whitespace in
                                                  // spec

                // Single character matches
                arguments("a b c a d.", //
                        new MatchSpec("", "a", "", ""), //
                        asList(new Range(0, 1), new Range(6, 7))), //

                // Empty result cases
                arguments("some text here", //
                        new MatchSpec("", "notfound", "", ""), //
                        asList()), //
                arguments("", //
                        new MatchSpec("", "foo", "", ""), //
                        asList()) // Empty document //
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
