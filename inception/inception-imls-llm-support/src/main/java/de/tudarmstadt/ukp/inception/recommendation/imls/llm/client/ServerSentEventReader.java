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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Turns the already-line-split body of a {@code text/event-stream} response into a lazy stream of
 * dispatched {@link ServerSentEvent}s, following the <a href=
 * "https://html.spec.whatwg.org/multipage/server-sent-events.html#event-stream-interpretation">WHATWG
 * event-stream interpretation</a>: fields accumulate until a blank line dispatches the event,
 * multiple {@code data:} fields fold into one payload joined with {@code \n}, a single leading
 * space after the colon is stripped, lines starting with {@code :} are comments, and unrecognized
 * fields are ignored. Events without a {@code data} field are not dispatched.
 * <p>
 * The framing is provider-neutral: it knows nothing about {@code [DONE]} sentinels or chunk JSON —
 * those are the calling adapter's concern.
 * <p>
 * Deviation from the spec: a pending event still buffered when the underlying line stream ends is
 * flushed rather than discarded. The spec discards an event not terminated by a blank line, but
 * real providers routinely omit the trailing blank line before closing the connection, so
 * discarding it would drop the last chunk.
 * <p>
 * The returned stream is lazy and pulls from the source on demand; closing it closes the source, so
 * a caller that breaks out early (e.g. on a {@code [DONE]} payload) stops reading the connection.
 */
public final class ServerSentEventReader
{
    private ServerSentEventReader()
    {
        // No instances
    }

    public static Stream<ServerSentEvent> parse(Stream<String> aLines)
    {
        var lineIterator = aLines.iterator();
        var eventIterator = new EventIterator(lineIterator);
        var spliterator = Spliterators.spliteratorUnknownSize(eventIterator,
                Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false).onClose(aLines::close);
    }

    private static final class EventIterator
        implements Iterator<ServerSentEvent>
    {
        private final Iterator<String> lines;
        private ServerSentEvent next;
        private boolean exhausted;

        EventIterator(Iterator<String> aLines)
        {
            lines = aLines;
        }

        @Override
        public boolean hasNext()
        {
            if (next == null && !exhausted) {
                next = readNext();
                if (next == null) {
                    exhausted = true;
                }
            }
            return next != null;
        }

        @Override
        public ServerSentEvent next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = next;
            next = null;
            return result;
        }

        /**
         * Reads lines until an event dispatches (blank line terminating a block that had a
         * {@code data} field) or the source is exhausted, flushing any trailing event on EOF.
         * Returns {@code null} once no further event can be produced.
         */
        private ServerSentEvent readNext()
        {
            var builder = new EventBuilder();

            while (lines.hasNext()) {
                var line = lines.next();

                if (line.isEmpty()) {
                    if (builder.hasData()) {
                        return builder.build();
                    }
                    // Blank line with nothing buffered (or a comment-only block): reset and keep
                    // looking rather than dispatching an empty event.
                    builder = new EventBuilder();
                    continue;
                }

                if (line.charAt(0) == ':') {
                    // Comment line
                    continue;
                }

                var colon = line.indexOf(':');
                String field;
                String value;
                if (colon < 0) {
                    field = line;
                    value = "";
                }
                else {
                    field = line.substring(0, colon);
                    value = line.substring(colon + 1);
                    // Strip a single leading space, per spec.
                    if (!value.isEmpty() && value.charAt(0) == ' ') {
                        value = value.substring(1);
                    }
                }

                builder.field(field, value);
            }

            // EOF: flush a trailing event that never got its terminating blank line.
            if (builder.hasData()) {
                return builder.build();
            }

            return null;
        }
    }

    private static final class EventBuilder
    {
        private String event;
        private StringBuilder data;
        private String id;
        private Long retry;

        boolean hasData()
        {
            return data != null;
        }

        void field(String aField, String aValue)
        {
            switch (aField) {
            case "event":
                event = aValue;
                break;
            case "data":
                if (data == null) {
                    data = new StringBuilder(aValue);
                }
                else {
                    data.append('\n').append(aValue);
                }
                break;
            case "id":
                // Per spec, an id containing a NUL is ignored; otherwise it sets the last event id.
                if (aValue.indexOf('\0') < 0) {
                    id = aValue;
                }
                break;
            case "retry":
                try {
                    retry = Long.valueOf(aValue);
                }
                catch (NumberFormatException e) {
                    // Ignore a non-integer retry, per spec.
                }
                break;
            default:
                // Unknown field: ignore.
                break;
            }
        }

        ServerSentEvent build()
        {
            return new ServerSentEvent(event, data == null ? null : data.toString(), id, retry);
        }
    }
}
