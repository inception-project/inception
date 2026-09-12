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

import static wicket.contrib.input.events.EventType.click;

import java.io.Serializable;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyCombo;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * The page-wide undo/redo keyboard shortcuts.
 */
public class UndoKeyBindingsPanel
    extends Panel
{
    private static final long serialVersionUID = -3111893017331010726L;

    private @SpringBean KeyBindingsProperties keyBindings;
    private @SpringBean AnnotationSchemaService schemaService;

    public UndoKeyBindingsPanel(String aId)
    {
        super(aId);

        setOutputMarkupId(true);

        add(bindShortcut("undo", () -> keyBindings.getEditing().getUndo(),
                (target, context) -> executor().actionUndo(this, target, context)));

        add(bindShortcut("redo", () -> keyBindings.getEditing().getRedo(),
                (target, context) -> executor().actionRedo(this, target, context)));
    }

    private LambdaAjaxLink bindShortcut(String aId, SerializableSupplier<KeyCombo> aCombo,
            UndoRedoAction aAction)
    {
        var link = new LambdaAjaxLink(aId, target -> onActiveEditor(target, aAction));
        link.add(aCombo.get().toInputBehavior(click));
        return link;
    }

    private void onActiveEditor(AjaxRequestTarget aTarget, UndoRedoAction aAction)
    {
        var context = getActiveContext().orElse(null);

        if (context == null || !context.getActionHandler().isEditable()) {
            return;
        }

        aAction.apply(aTarget, context);
    }

    private Optional<DiamContext> getActiveContext()
    {
        var manager = findParent(DocumentEditorManager.class);
        return manager != null ? manager.getActiveContext() : Optional.empty();
    }

    private UndoRedoActionExecutor executor()
    {
        return new UndoRedoActionExecutor(schemaService);
    }

    @FunctionalInterface
    private interface UndoRedoAction
        extends Serializable
    {
        void apply(AjaxRequestTarget aTarget, DiamContext aContext);
    }
}
