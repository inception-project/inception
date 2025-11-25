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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.log;

import java.io.IOException;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event.RelationEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.log.model.RelationDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class RelationEventAdapter
    implements EventLoggingAdapter<RelationEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return RelationEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public String getDetails(RelationEvent aEvent) throws IOException
    {
        var source = new AnnotationDetails(aEvent.getSourceAnnotation());
        var target = new AnnotationDetails(aEvent.getTargetAnnotation());
        var details = new RelationDetails(aEvent.getAnnotation(), source, target);
        return JSONUtil.toJsonString(details);
    }

    @Override
    public long getDocument(RelationEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public String getAnnotator(RelationEvent aEvent)
    {
        return aEvent.getDocumentOwner();
    }

    @Override
    public long getProject(RelationEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }
}
