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
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
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

    private final SerializableSupplier<DiamContext> editorContextSupplier;

    private final ContextMenu contextMenu;

    /**
     * @param aContext
     *            the editor context.
     * @deprecated Use {@link DiamAjaxBehavior#DiamAjaxBehavior(SerializableSupplier)} instead
     */
    @Deprecated
    public DiamAjaxBehavior(DiamContext aContext)
    {
        this(aContext, null);
    }

    /**
     * @param aContext
     *            the editor context.
     * @param aContextMenu
     *            the context menu to use, may be {@code null}.
     * @deprecated Use {@link DiamAjaxBehavior#DiamAjaxBehavior(SerializableSupplier, ContextMenu)}
     *             instead
     */
    @Deprecated
    public DiamAjaxBehavior(DiamContext aContext, ContextMenu aContextMenu)
    {
        Validate.notNull(aContext, "DiamContext must be set");

        contextMenu = aContextMenu;
        editorContextSupplier = () -> aContext;
    }

    /**
     * @param aContextSupplier
     *            resolves the editor context.
     */
    public DiamAjaxBehavior(SerializableSupplier<DiamContext> aContextSupplier)
    {
        this(aContextSupplier, null);
    }

    /**
     * @param aContextSupplier
     *            resolves the editor context.
     * @param aContextMenu
     *            the context menu to use, may be {@code null}.
     */
    public DiamAjaxBehavior(SerializableSupplier<DiamContext> aContextSupplier,
            ContextMenu aContextMenu)
    {
        Validate.notNull(aContextSupplier, "A DiamContext supplier must be set");

        contextMenu = aContextMenu;
        editorContextSupplier = aContextSupplier;
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
     * @return the editor context.
     */
    public DiamContext getContext()
    {
        var context = editorContextSupplier.get();

        Validate.notNull(context, "No DiamContext available - there is no active editor");

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
        var context = editorContextSupplier.get();
        if (context == null) {
            LOG.debug("Ignoring DIAM request: there is no active editor");
            return;
        }

        var diamRequest = new DiamRequest(context, this, RequestCycle.get().getRequest());

        var priorityHandler = priorityHandlers.stream() //
                .filter(handler -> handler.accepts(diamRequest)) //
                .findFirst();

        if (priorityHandler.isPresent()) {
            call(aTarget, priorityHandler.get(), diamRequest);
            return;
        }

        if (globalHandlersEnabled) {
            handlers.getHandler(diamRequest) //
                    .ifPresent(h -> call(aTarget, h, diamRequest));
        }
    }

    private void call(AjaxRequestTarget aTarget, EditorAjaxRequestHandler aHandler,
            DiamRequest aRequest)
    {
        LOG.trace("AJAX request received for {}", aHandler.getClass().getName());
        try (var watch = new ServerTimingWatch("diam", "diam (" + aHandler.getCommand() + ")")) {
            aHandler.handle(aRequest, aTarget);
            return;
        }
    }

    @Override
    public ContextMenu getContextMenu()
    {
        return contextMenu;
    }
}
