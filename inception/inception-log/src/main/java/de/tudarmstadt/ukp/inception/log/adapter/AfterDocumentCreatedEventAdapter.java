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
package de.tudarmstadt.ukp.inception.log.adapter;

import java.io.IOException;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class AfterDocumentCreatedEventAdapter
    implements EventLoggingAdapter<AfterDocumentCreatedEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return AfterDocumentCreatedEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getDocument(AfterDocumentCreatedEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(AfterDocumentCreatedEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getDetails(AfterDocumentCreatedEvent aEvent) throws IOException
    {
        var details = new Details();
        details.documentName = aEvent.getDocument().getName();
        details.format = aEvent.getDocument().getFormat();
        details.state = aEvent.getDocument().getState();
        return JSONUtil.toJsonString(details);
    }

    public static class Details
    {
        public String documentName;
        public String format;
        public SourceDocumentState state;
    }
}
