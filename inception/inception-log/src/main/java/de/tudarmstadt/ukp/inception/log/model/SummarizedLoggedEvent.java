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
package de.tudarmstadt.ukp.inception.log.model;

import java.time.Instant;

public class SummarizedLoggedEvent
{
    private final long document;
    private final String event;
    private final Instant date;
    private final long count;

    public SummarizedLoggedEvent(String aEvent, long aDocument, Instant aDate, long aCount)
    {
        document = aDocument;
        event = aEvent;
        date = aDate;
        count = aCount;
    }

    public Instant getDate()
    {
        return date;
    }

    public long getCount()
    {
        return count;
    }

    public String getEvent()
    {
        return event;
    }

    public long getDocument()
    {
        return document;
    }
}
