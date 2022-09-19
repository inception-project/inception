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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import org.apache.wicket.Page;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationAutoConfiguration#renderNotificationRenderStep}.
 * </p>
 */
@Order(RenderStep.RENDER_NOTIFICATION)
public class RenderNotificationRenderStep
    implements RenderStep
{
    public static final String ID = "RenderNotificationRenderStep";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest)
    {
        // Fire render event into UI
        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle == null) {
            return;
        }

        requestCycle.find(IPageRequestHandler.class).ifPresent(handler -> {
            Page page = (Page) handler.getPage();
            page.send(page, Broadcast.BREADTH,
                    new RenderAnnotationsEvent(aRequest.getCas(), aRequest, aVDoc));
        });
    }
}
