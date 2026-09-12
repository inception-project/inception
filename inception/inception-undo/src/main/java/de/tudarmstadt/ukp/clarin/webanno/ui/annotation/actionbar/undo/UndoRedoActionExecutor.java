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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo;

import static de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil.handleException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RedoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationAction;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Performs undo/redo against a given editor context.
 * <p>
 * This lives outside the panels because two of them drive it and they differ only in how they pick
 * the target: {@link UndoPanel}'s buttons act on the editor they belong to, while
 * {@link UndoKeyBindingsPanel}'s page-wide shortcut acts on whichever editor is active. The state
 * itself is per-editor either way - see {@link #getState}.
 */
public class UndoRedoActionExecutor
    implements Serializable
{
    private static final long serialVersionUID = 2952481047181337301L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1);

    private final AnnotationSchemaService schemaService;

    public UndoRedoActionExecutor(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    private Component getStateOwner(Component aHost, DiamContext aContext)
    {
        return aContext instanceof Component component ? component : aHost.getPage();
    }

    public UndoRedoState getState(Component aHost, DiamContext aContext)
    {
        var owner = getStateOwner(aHost, aContext);
        var state = owner.getMetaData(UndoRedoStateKey.INSTANCE);
        if (state == null) {
            state = new UndoRedoState();
            owner.setMetaData(UndoRedoStateKey.INSTANCE, state);
        }
        return state;
    }

    public long getRequestId()
    {
        var requestId = RequestCycle.get().getMetaData(RequestIdKey.INSTANCE);
        if (requestId == null) {
            requestId = NEXT_REQUEST_ID.getAndIncrement();
            RequestCycle.get().setMetaData(RequestIdKey.INSTANCE, requestId);
        }
        return requestId;
    }

    public void clearState(Component aHost, DiamContext aContext)
    {
        getStateOwner(aHost, aContext).setMetaData(UndoRedoStateKey.INSTANCE, null);
    }

    public void actionUndo(Component aHost, AjaxRequestTarget aTarget, DiamContext aContext)
    {
        var state = getState(aHost, aContext);

        if (!state.hasUndoableActions()) {
            aHost.info("There are no un-doable actions");
            aTarget.addChildren(aHost.getPage(), IFeedback.class);
            return;
        }

        try {
            aContext.getActionHandler().ensureIsEditable();
        }
        catch (AnnotationException e) {
            aHost.error(e.getMessage());
            aTarget.addChildren(aHost.getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            var cas = aContext.getEditorCas();
            var action = state.popUndoable();
            var requestId = action.getRequestId();
            var postAction = Optional.<PostAction> empty();
            var messages = new ArrayList<LogMessage>();

            while (true) {
                postAction = action.undo(schemaService, cas, messages);

                messages.forEach(msg -> msg.toWicket(aHost.getPage()));
                messages.clear();

                if (action instanceof RedoableAnnotationAction) {
                    state.pushRedoable((RedoableAnnotationAction) action);
                }

                if (state.peekUndoable().map($ -> $.getRequestId() != requestId).orElse(true)) {
                    break;
                }

                action = state.popUndoable();
            }

            postAction.ifPresent($ -> $.apply(aHost, aContext, aTarget));
            aTarget.addChildren(aHost.getPage(), IFeedback.class);

            aContext.getActionHandler().writeEditorCas(cas);
            aContext.actionRefreshDocument(aTarget);
        }
        catch (IOException | AnnotationException e) {
            clearState(aHost, aContext);
            handleException(LOG, aHost.getPage(), aTarget, e);
        }
        finally {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, false);
        }
    }

    public void actionRedo(Component aHost, AjaxRequestTarget aTarget, DiamContext aContext)
    {
        var state = getState(aHost, aContext);

        if (!state.hasRedoableActions()) {
            aHost.info("There are no re-doable actions");
            aTarget.addChildren(aHost.getPage(), IFeedback.class);
            return;
        }

        try {
            aContext.getActionHandler().ensureIsEditable();
        }
        catch (AnnotationException e) {
            aHost.error(e.getMessage());
            aTarget.addChildren(aHost.getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            var cas = aContext.getEditorCas();
            var action = state.popRedoable();
            var requestId = action.getRequestId();
            var postAction = Optional.<PostAction> empty();
            var messages = new ArrayList<LogMessage>();

            while (true) {
                postAction = action.redo(schemaService, cas, messages);

                messages.forEach(msg -> msg.toWicket(aHost.getPage()));
                messages.clear();

                if (action instanceof UndoableAnnotationAction) {
                    state.pushUndoable((UndoableAnnotationAction) action);
                }

                if (state.peekRedoable().map($ -> $.getRequestId() != requestId).orElse(true)) {
                    break;
                }

                action = state.popRedoable();
            }

            postAction.ifPresent($ -> $.apply(aHost, aContext, aTarget));
            aTarget.addChildren(aHost.getPage(), IFeedback.class);

            aContext.getActionHandler().writeEditorCas(cas);
            aContext.actionRefreshDocument(aTarget);
        }
        catch (IOException | AnnotationException e) {
            clearState(aHost, aContext);
            handleException(LOG, aHost.getPage(), aTarget, e);
        }
        finally {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, false);
        }
    }
}
