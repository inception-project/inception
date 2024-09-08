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
package de.tudarmstadt.ukp.inception.ui.core.docanno.log;

import java.io.IOException;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.ui.core.docanno.event.DocumentMetadataEvent;

@Component
public class DocumentMetadataEventAdapter
    implements EventLoggingAdapter<DocumentMetadataEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return DocumentMetadataEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getDocument(DocumentMetadataEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(DocumentMetadataEvent aEvent)
    {
        return aEvent.getProject().getId();
    }

    @Override
    public String getAnnotator(DocumentMetadataEvent aEvent)
    {
        return aEvent.getDocumentOwner();
    }

    @Override
    public String getDetails(DocumentMetadataEvent aEvent) throws IOException
    {
        AnnotationDetails details = new AnnotationDetails(aEvent.getAnnotation());
        return JSONUtil.toJsonString(details);
    }
}
