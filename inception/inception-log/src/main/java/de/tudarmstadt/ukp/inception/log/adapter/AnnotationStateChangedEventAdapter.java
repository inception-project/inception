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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;

import java.io.IOException;
import java.util.Objects;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.documents.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.inception.log.model.StateChangeDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class AnnotationStateChangedEventAdapter
    implements EventLoggingAdapter<AnnotationStateChangeEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return AnnotationStateChangeEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public String getAnnotator(AnnotationStateChangeEvent aEvent)
    {
        return aEvent.getAnnotationDocument().getUser();
    }

    @Override
    public long getDocument(AnnotationStateChangeEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(AnnotationStateChangeEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getUser(AnnotationStateChangeEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getDetails(AnnotationStateChangeEvent aEvent) throws IOException
    {
        var details = new StateChangeDetails();
        details.setState(Objects.toString(aEvent.getNewState(), null));
        details.setPreviousState(Objects.toString(aEvent.getPreviousState(), null));

        var annotatorState = aEvent.getAnnotationDocument().getAnnotatorState();
        details.setAnnotatorState(Objects.toString(annotatorState, null));
        if (annotatorState == FINISHED || annotatorState == IGNORE) {
            details.setAnnotatorComment(aEvent.getAnnotationDocument().getAnnotatorComment());
        }

        return JSONUtil.toJsonString(details);
    }
}
