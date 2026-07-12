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

/**
 * A single dispatched server-sent event, as interpreted per the
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">WHATWG event-stream
 * specification</a>. Multi-line {@code data:} fields have already been folded into {@link #data()}
 * (joined with {@code \n}). Any field absent from the event is {@code null}.
 *
 * @param event
 *            the {@code event:} field, or {@code null} (the spec default type is {@code message})
 * @param data
 *            the folded {@code data:} payload; never {@code null} for a dispatched event (events
 *            without a data field are not dispatched)
 * @param id
 *            the {@code id:} field, or {@code null}
 * @param retry
 *            the {@code retry:} reconnection time in milliseconds, or {@code null} if absent or not
 *            a valid integer
 */
public record ServerSentEvent( //
        String event, //
        String data, //
        String id, //
        Long retry)
{}
