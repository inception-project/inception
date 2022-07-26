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

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainSpanEvent;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;

@Component
public class ChainSpanEventAdapter
    implements EventLoggingAdapter<ChainSpanEvent>
{
    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof ChainSpanEvent;
    }

    @Override
    public long getDocument(ChainSpanEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(ChainSpanEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getAnnotator(ChainSpanEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getDetails(ChainSpanEvent aEvent) throws IOException
    {
        AnnotationDetails details = new AnnotationDetails(aEvent.getAnnotation());
        return JSONUtil.toJsonString(details);
    }
}
