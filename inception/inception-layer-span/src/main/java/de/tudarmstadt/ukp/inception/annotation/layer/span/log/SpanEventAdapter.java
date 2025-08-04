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
package de.tudarmstadt.ukp.inception.annotation.layer.span.log;

import java.io.IOException;

import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class SpanEventAdapter
    implements EventLoggingAdapter<SpanEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return SpanEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getDocument(SpanEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(SpanEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getAnnotator(SpanEvent aEvent)
    {
        return aEvent.getDocumentOwner();
    }

    @Override
    public String getDetails(SpanEvent aEvent) throws IOException
    {
        var details = new AnnotationDetails(aEvent.getAnnotation());
        return JSONUtil.toJsonString(details);
    }
}
