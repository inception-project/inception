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

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketExceptionUtil.handleException;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.Shift;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.input.InputBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.CreateRelationAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.CreateSpanAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.DeleteRelationAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.DeleteSpanAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RedoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UpdateFeatureValueAnnotationAction;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanDeletedEvent;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import wicket.contrib.input.events.key.KeyType;

public class UndoPanel
    extends Panel
{
    private static final long serialVersionUID = -6213541738534665790L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService schemaService;

    private final Map<Class<? extends AnnotationEvent>, SerializableBiFunction<AnnotationSchemaService, AnnotationEvent, UndoableAnnotationAction>> undoHandlers;

    public UndoPanel(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        undoHandlers = new HashMap<>();

        registerHandler(SpanCreatedEvent.class, CreateSpanAnnotationAction::new);
        registerHandler(SpanDeletedEvent.class, DeleteSpanAnnotationAction::new);
        registerHandler(RelationCreatedEvent.class, CreateRelationAnnotationAction::new);
        registerHandler(RelationDeletedEvent.class, DeleteRelationAnnotationAction::new);
        registerHandler(FeatureValueUpdatedEvent.class, UpdateFeatureValueAnnotationAction::new);

        queue(new LambdaAjaxLink("undo", this::actionUndo)
                .add(new InputBehavior(new KeyType[] { Ctrl, KeyType.z }, click)));
        queue(new LambdaAjaxLink("redo", this::actionRedo)
                .add(new InputBehavior(new KeyType[] { Shift, Ctrl, KeyType.z }, click)));

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends AnnotationEvent> void registerHandler(Class<T> aEventClass,
            SerializableBiFunction<AnnotationSchemaService, T, UndoableAnnotationAction> aHandler)
    {
        undoHandlers.put(aEventClass, (SerializableBiFunction) aHandler);
    }

    private UndoRedoState getState()
    {
        // We keep the state in the page because the undo panel is part of the list view in the
        // action bar and looses its state when the page is reloaded
        UndoRedoState state = getPage().getMetaData(UndoRedoStateKey.INSTANCE);
        if (state == null) {
            state = new UndoRedoState();
            getPage().setMetaData(UndoRedoStateKey.INSTANCE, state);
        }
        return state;
    }

    private void clearState()
    {
        getPage().setMetaData(UndoRedoStateKey.INSTANCE, null);
    }

    private void actionUndo(AjaxRequestTarget aTarget)
    {
        if (getState().getUndoableActions().isEmpty()) {
            info("There are no un-doable actions");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            AnnotationPageBase page = findParent(AnnotationPageBase.class);

            var action = getState().getUndoableActions().poll();

            // select + scroll to (on create)
            // only scroll to and maybe highlight where the annotation was (on delete)
            // nothing (???)

            var cas = page.getEditorCas();
            var postAction = action.undo(schemaService, cas);
            page.writeEditorCas(cas);

            if (action instanceof RedoableAnnotationAction) {
                getState().getRedoableActions().push((RedoableAnnotationAction) action);
            }

            postAction.ifPresent($ -> $.apply(this, aTarget));

            page.actionRefreshDocument(aTarget);
        }
        catch (IOException | AnnotationException e) {
            handleException(LOG, getPage(), aTarget, e);
        }
        finally {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, false);
        }
    }

    private void actionRedo(AjaxRequestTarget aTarget)
    {
        if (getState().getRedoableActions().isEmpty()) {
            info("There are no re-doable actions");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            AnnotationPageBase page = findParent(AnnotationPageBase.class);

            var action = getState().getRedoableActions().poll();

            var cas = page.getEditorCas();
            var postAction = action.redo(schemaService, cas);
            page.writeEditorCas(cas);

            if (action instanceof UndoableAnnotationAction) {
                getState().getUndoableActions().push((UndoableAnnotationAction) action);
            }

            postAction.ifPresent($ -> $.apply(this, aTarget));

            page.actionRefreshDocument(aTarget);
        }
        catch (IOException | AnnotationException e) {
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

        var handler = undoHandlers.get(aEvent.getClass());

        if (handler != null) {
            getState().getRedoableActions().clear();
            getState().getUndoableActions().push(handler.apply(schemaService, aEvent));
        }
    }
}
