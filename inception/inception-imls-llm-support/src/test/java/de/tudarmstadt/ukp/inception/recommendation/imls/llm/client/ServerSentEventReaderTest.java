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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ServerSentEventReaderTest
{
    private static List<ServerSentEvent> parse(String... aLines)
    {
        return ServerSentEventReader.parse(Stream.of(aLines)).toList();
    }

    @Test
    void thatBlankLineDispatchesEvent()
    {
        var events = parse( //
                "data: first", //
                "", //
                "data: second", //
                "");

        assertThat(events).extracting(ServerSentEvent::data) //
                .containsExactly("first", "second");
    }

    @Test
    void thatMultipleDataLinesAreFoldedWithNewline()
    {
        var events = parse( //
                "data: line one", //
                "data: line two", //
                "");

        assertThat(events).extracting(ServerSentEvent::data) //
                .containsExactly("line one\nline two");
    }

    @Test
    void thatEventAndIdFieldsAreParsed()
    {
        var events = parse( //
                "event: message", //
                "id: 42", //
                "data: hello", //
                "");

        assertThat(events)
                .extracting(ServerSentEvent::event, ServerSentEvent::id, ServerSentEvent::data)
                .containsExactly(tuple("message", "42", "hello"));
    }

    @Test
    void thatCommentLinesAreIgnored()
    {
        var events = parse( //
                ": this is a comment", //
                "data: payload", //
                "");

        assertThat(events).extracting(ServerSentEvent::data).containsExactly("payload");
    }

    @Test
    void thatOnlySingleLeadingSpaceIsStripped()
    {
        var events = parse( //
                "data:  two-leading-spaces", //
                "");

        // One leading space stripped, the second retained.
        assertThat(events).extracting(ServerSentEvent::data).containsExactly(" two-leading-spaces");
    }

    @Test
    void thatFieldWithoutColonHasEmptyValue()
    {
        var events = parse( //
                "data", //
                "");

        assertThat(events).extracting(ServerSentEvent::data).containsExactly("");
    }

    @Test
    void thatEventWithoutDataIsNotDispatched()
    {
        var events = parse( //
                "event: ping", //
                "", //
                "data: real", //
                "");

        assertThat(events).extracting(ServerSentEvent::data).containsExactly("real");
    }

    @Test
    void thatTrailingEventWithoutBlankLineIsFlushed()
    {
        var events = parse( //
                "data: no-terminating-blank");

        assertThat(events).extracting(ServerSentEvent::data)
                .containsExactly("no-terminating-blank");
    }

    @Test
    void thatUnknownFieldsAreIgnored()
    {
        var events = parse( //
                "foo: bar", //
                "data: kept", //
                "");

        assertThat(events).extracting(ServerSentEvent::data).containsExactly("kept");
    }

    @Test
    void thatRetryIsParsedAndInvalidRetryIgnored()
    {
        var valid = parse("retry: 3000", "data: x", "");
        assertThat(valid).extracting(ServerSentEvent::retry).containsExactly(3000L);

        var invalid = parse("retry: soon", "data: x", "");
        assertThat(invalid).extracting(ServerSentEvent::retry).containsOnlyNulls();
    }
}
