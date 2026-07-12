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

import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;
import de.tudarmstadt.ukp.inception.diam.model.DiamContext;
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

    private DiamContext context;

    private final ContextMenu contextMenu;

    public DiamAjaxBehavior(DiamContext aContext)
    {
        this(aContext, null);
    }

    public DiamAjaxBehavior(DiamContext aContext, ContextMenu aContextMenu)
    {
        Validate.notNull(aContext, "DiamContext must be set");

        contextMenu = aContextMenu;
        context = aContext;
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

    /**
     * @return the editor-scoped context through which handlers resolve state, CAS, action handler
     *         and editability. Never {@code null} — it is mandatory and set at construction.
     */
    public DiamContext getContext()
    {
        return context;
    }

    @Override
    protected void onBind()
    {
        super.onBind();
    }

    @Override
    protected void respond(AjaxRequestTarget aTarget)
    {
        var diamRequest = new DiamRequest(context, RequestCycle.get().getRequest());

        var priorityHandler = priorityHandlers.stream() //
                .filter(handler -> handler.accepts(diamRequest)) //
                .findFirst();

        if (priorityHandler.isPresent()) {
            call(aTarget, priorityHandler.get());
            return;
        }

        if (globalHandlersEnabled) {
            handlers.getHandler(diamRequest) //
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
