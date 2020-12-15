/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.log.adapter;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.log.model.StateChangeDetails;

@Component
public class AnnotationStateChangedEventAdapter
    implements EventLoggingAdapter<AnnotationStateChangeEvent>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof AnnotationStateChangeEvent;
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
    public String getDetails(AnnotationStateChangeEvent aEvent)
    {
        try {
            StateChangeDetails details = new StateChangeDetails();
            details.setState(Objects.toString(aEvent.getNewState(), null));
            details.setPreviousState(Objects.toString(aEvent.getPreviousState(), null));
            return JSONUtil.toJsonString(details);
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
    }
}
