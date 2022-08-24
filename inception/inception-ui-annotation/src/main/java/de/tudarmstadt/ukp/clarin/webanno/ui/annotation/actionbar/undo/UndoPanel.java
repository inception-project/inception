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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.CreateRelationAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.CreateSpanAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.DeleteRelationAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.DeleteSpanAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RedoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationAction;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanDeletedEvent;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public class UndoPanel
    extends Panel
{
    private static final long serialVersionUID = -6213541738534665790L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService schemaService;

    private final Deque<UndoableAnnotationAction> undoableActions;
    private final Deque<RedoableAnnotationAction> redoableActions;
    private final Map<Class<? extends AnnotationEvent>, SerializableFunction<AnnotationEvent, UndoableAnnotationAction>> undoHandlers;

    public UndoPanel(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        undoableActions = new LinkedList<>();
        redoableActions = new LinkedList<>();
        undoHandlers = new HashMap<>();

        registerHandler(SpanCreatedEvent.class, CreateSpanAnnotationAction::new);
        registerHandler(SpanDeletedEvent.class, DeleteSpanAnnotationAction::new);
        registerHandler(RelationCreatedEvent.class, CreateRelationAnnotationAction::new);
        registerHandler(RelationDeletedEvent.class, DeleteRelationAnnotationAction::new);

        queue(new LambdaAjaxLink("undo", this::actionUndo));
        queue(new LambdaAjaxLink("redo", this::actionRedo));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T extends AnnotationEvent> void registerHandler(Class<T> aEventClass,
            SerializableFunction<T, UndoableAnnotationAction> aHandler)
    {
        undoHandlers.put(aEventClass, (SerializableFunction) aHandler);
    }

    private void actionUndo(AjaxRequestTarget aTarget)
    {
        if (undoableActions.isEmpty()) {
            info("There are no un-doable actions");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            AnnotationPageBase page = findParent(AnnotationPageBase.class);

            var action = undoableActions.poll();

            var cas = page.getEditorCas();
            action.undo(schemaService, cas);
            page.writeEditorCas(cas);

            if (action instanceof RedoableAnnotationAction) {
                redoableActions.push((RedoableAnnotationAction) action);
            }

            page.getAnnotationActionHandler().actionSelectAndJump(aTarget, action.getVid());
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
        if (redoableActions.isEmpty()) {
            info("There are no re-doable actions");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            RequestCycle.get().setMetaData(PerformingUndoRedoAction.INSTANCE, true);

            AnnotationPageBase page = findParent(AnnotationPageBase.class);

            var action = redoableActions.poll();

            var cas = page.getEditorCas();
            action.redo(schemaService, cas);
            page.writeEditorCas(cas);

            if (action instanceof UndoableAnnotationAction) {
                undoableActions.push((UndoableAnnotationAction) action);
            }

            page.getAnnotationActionHandler().actionSelectAndJump(aTarget, action.getVid());
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
        redoableActions.clear();
        undoableActions.clear();
    }

    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        var flag = RequestCycle.get().getMetaData(PerformingUndoRedoAction.INSTANCE);
        if (flag != null && flag) {
            return;
        }

        redoableActions.clear();

        var handler = undoHandlers.get(aEvent.getClass());

        if (handler != null) {
            undoableActions.push(handler.apply(aEvent));
        }
    }
}
