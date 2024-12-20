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
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.support.http.ServerTimingWatch;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;

public class DiamAjaxBehavior
    extends AbstractDefaultAjaxBehavior
    implements ContextMenuLookup
{
    private static final long serialVersionUID = -7681019566646236763L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean EditorAjaxRequestHandlerExtensionPoint handlers;

    private List<EditorAjaxRequestHandler> priorityHandlers = new ArrayList<>();

    private boolean globalHandlersEnabled = true;

    private final ContextMenu contextMenu;

    public DiamAjaxBehavior()
    {
        this(null);
    }

    public DiamAjaxBehavior(ContextMenu aContextMenu)
    {
        contextMenu = aContextMenu;
    }

    public DiamAjaxBehavior addPriorityHandler(EditorAjaxRequestHandler aHandler)
    {
        priorityHandlers.add(aHandler);
        return this;
    }

    public DiamAjaxBehavior setGlobalHandlersEnabled(boolean aGlobalHandlersEnabled)
    {
        globalHandlersEnabled = aGlobalHandlersEnabled;
        return this;
    }

    @Override
    protected void onBind()
    {
        super.onBind();
    }

    @Override
    protected void respond(AjaxRequestTarget aTarget)
    {
        var request = RequestCycle.get().getRequest();

        var priorityHandler = priorityHandlers.stream() //
                .filter(handler -> handler.accepts(request)) //
                .findFirst();

        if (priorityHandler.isPresent()) {
            call(aTarget, priorityHandler.get());
            return;
        }

        if (globalHandlersEnabled) {
            handlers.getHandler(request) //
                    .ifPresent(h -> call(aTarget, h));
        }
    }

    private void call(AjaxRequestTarget aTarget, EditorAjaxRequestHandler aHandler)
    {
        LOG.trace("AJAX request received for {}", aHandler.getClass().getName());
        var request = RequestCycle.get().getRequest();
        try (var watch = new ServerTimingWatch("diam", "diam (" + aHandler.getCommand() + ")")) {
            aHandler.handle(this, aTarget, request);
            return;
        }
    }

    @Override
    public ContextMenu getContextMenu()
    {
        return contextMenu;
    }
}
