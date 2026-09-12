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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyCombo;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableActionSupportRegistry;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class UndoPanel
    extends Panel
{
    private static final long serialVersionUID = -6213541738534665790L;

    private @SpringBean KeyBindingsProperties keyBindings;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean UndoableActionSupportRegistry undoableActionSupportRegistry;

    private final DiamContext context;

    public UndoPanel(String aId, DiamContext aContext)
    {
        super(aId);

        context = aContext;

        queue(withShortcutHint(
                new LambdaAjaxLink("undo", t -> executor().actionUndo(this, t, context)),
                () -> keyBindings.getEditing().getUndo()));
        queue(withShortcutHint(
                new LambdaAjaxLink("redo", t -> executor().actionRedo(this, t, context)),
                () -> keyBindings.getEditing().getRedo()));
    }

    private LambdaAjaxLink withShortcutHint(LambdaAjaxLink aLink,
            SerializableSupplier<KeyCombo> aCombo)
    {
        aLink.add(AttributeModifier.append("title",
                () -> " (" + KeyBindingsUtil.formatShortcut(aCombo.get()) + ")"));
        return aLink;
    }

    private UndoRedoActionExecutor executor()
    {
        return new UndoRedoActionExecutor(schemaService);
    }

    @OnEvent
    public void onDocumentOpenedEvent(DocumentOpenedEvent aEvent)
    {
        executor().clearState(this, context);
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
            var executor = executor();
            executor.getState(this, context).clearRedoableActions();
            long requestId = executor.getRequestId();
            try {
                executor.getState(this, context)
                        .pushUndoable(handler.get().actionForEvent(requestId, aEvent));
            }
            catch (IllegalArgumentException e) {
                // Ignore - undo not supported for this action...
            }
        }
    }
}
