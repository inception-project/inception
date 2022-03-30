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
package de.tudarmstadt.ukp.inception.diam.editor;

import java.lang.invoke.MethodHandles;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;

public class DiamAjaxBehavior
    extends AbstractDefaultAjaxBehavior
{
    private static final long serialVersionUID = -7681019566646236763L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean EditorAjaxRequestHandlerExtensionPoint handlers;

    @Override
    protected void onBind()
    {
        super.onBind();
    }

    @Override
    protected void respond(AjaxRequestTarget aTarget)
    {
        LOG.trace("AJAX request recieved");

        Request request = RequestCycle.get().getRequest();
        handlers.getHandler(request) //
                .ifPresent(h -> h.handle(aTarget, request));
    }
}
