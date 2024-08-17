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
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.Shift;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RedoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableActionSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationAction;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class UndoPanel
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -6213541738534665790L;

    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1);

    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean UndoableActionSupportRegistry undoableActionSupportRegistry;

    public UndoPanel(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        queue(new LambdaAjaxLink("undo", this::actionUndo)
                .add(new InputBehavior(new KeyType[] { Ctrl, KeyType.z }, click)));
        queue(new LambdaAjaxLink("redo", this::actionRedo)
                .add(new InputBehavior(new KeyType[] { Shift, Ctrl, KeyType.z }, click)));

    }

    private UndoRedoState getState()
    {
        // We keep the state in the page because the undo panel is part of the list view in the
        // action bar and looses its state when the page is reloaded
        var state = getPage().getMetaData(UndoRedoStateKey.INSTANCE);
        if (state == null) {
            state = new UndoRedoState();
            getPage().setMetaData(UndoRedoStateKey.INSTANCE, state);
        }
        return state;
    }

    private long getRequestId()
    {
        var requestId = getRequestCycle().getMetaData(RequestIdKey.INSTANCE);
        if (requestId == null) {
            requestId = NEXT_REQUEST_ID.getAndIncrement();
            getRequestCycle().setMetaData(RequestIdKey.INSTANCE, requestId);
        }
        return requestId;
    }

    private void clearState()
    {
        getPage().setMetaData(UndoRedoStateKey.INSTANCE, null);
    }

    private void actionUndo(AjaxRequestTarget aTarget)
    {
        var state = getState();

        if (!state.hasUndoableActions()) {
            info("There are no un-doable actions");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            var page = findParent(AnnotationPageBase.class);
            var cas = page.getEditorCas();
            var action = state.popUndoable();
            var requestId = action.getRequestId();
            var postAction = Optional.<PostAction> empty();
            var messages = new ArrayList<LogMessage>();

            while (true) {
                postAction = action.undo(schemaService, cas, messages);

                messages.forEach(msg -> msg.toWicket(getPage()));
                messages.clear();

                if (action instanceof RedoableAnnotationAction) {
                    state.pushRedoable((RedoableAnnotationAction) action);
                }

                if (state.peekUndoable().map($ -> $.getRequestId() != requestId).orElse(true)) {
                    break;
                }

                action = state.popUndoable();
            }

            postAction.ifPresent($ -> $.apply(this, aTarget));
            aTarget.addChildren(page, IFeedback.class);

            page.writeEditorCas(cas);
            page.actionRefreshDocument(aTarget);
        }
        catch (IOException | AnnotationException e) {
            clearState();
            handleException(LOG, getPage(), aTarget, e);
        }
        finally {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, false);
        }
    }

    private void actionRedo(AjaxRequestTarget aTarget)
    {
        var state = getState();

        if (!state.hasRedoableActions()) {
            info("There are no re-doable actions");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            var page = findParent(AnnotationPageBase.class);
            var cas = page.getEditorCas();
            var action = state.popRedoable();
            var requestId = action.getRequestId();
            var postAction = Optional.<PostAction> empty();
            var messages = new ArrayList<LogMessage>();

            while (true) {
                postAction = action.redo(schemaService, cas, messages);

                messages.forEach(msg -> msg.toWicket(getPage()));
                messages.clear();

                if (action instanceof UndoableAnnotationAction) {
                    state.pushUndoable((UndoableAnnotationAction) action);
                }

                if (state.peekRedoable().map($ -> $.getRequestId() != requestId).orElse(true)) {
                    break;
                }

                action = state.popRedoable();
            }

            postAction.ifPresent($ -> $.apply(this, aTarget));
            aTarget.addChildren(page, IFeedback.class);

            page.writeEditorCas(cas);
            page.actionRefreshDocument(aTarget);
        }
        catch (IOException | AnnotationException e) {
            clearState();
            handleException(LOG, getPage(), aTarget, e);
        }
        finally {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, false);
        }
    }

    @OnEvent
    public void onDocumentOpenedEvent(DocumentOpenedEvent aEvent)
    {
        clearState();
    }

    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        var flag = RequestCycle.get().getMetaData(PerformingUndoRedoAction.INSTANCE);
        if (flag != null && flag) {
            return;
        }

        var handler = undoableActionSupportRegistry.getExtension(aEvent);

        if (handler.isPresent()) {
            getState().clearRedoableActions();
            long requestId = getRequestId();
            try {
                getState().pushUndoable(handler.get().actionForEvent(requestId, aEvent));
            }
            catch (IllegalArgumentException e) {
                // Ignore - undo not supported for this action...
            }
        }
    }
}
