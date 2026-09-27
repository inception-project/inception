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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import static wicket.contrib.input.events.EventType.click;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyCombo;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class PagingKeyBindingsPanel
    extends Panel
{
    private static final long serialVersionUID = 5883231553828329147L;

    private @SpringBean KeyBindingsProperties keyBindings;

    private final DocumentEditorManager manager;

    public PagingKeyBindingsPanel(String aId, DocumentEditorManager aManager)
    {
        super(aId);

        manager = aManager;

        setOutputMarkupId(true);

        add(bindShortcut("showNext", () -> keyBindings.getNavigation().getNextPage(),
                (target, context) -> context.getViewState().moveToNextPage(context.getEditorCas(),
                        FocusPosition.TOP)));

        add(bindShortcut("showPrevious", () -> keyBindings.getNavigation().getPreviousPage(),
                (target, context) -> context.getViewState()
                        .moveToPreviousPage(context.getEditorCas(), FocusPosition.TOP)));

        add(bindShortcut("showFirst", () -> keyBindings.getNavigation().getFirstPage(),
                (target, context) -> context.getViewState().moveToFirstPage(context.getEditorCas(),
                        FocusPosition.TOP)));

        add(bindShortcut("showLast", () -> keyBindings.getNavigation().getLastPage(),
                (target, context) -> context.getViewState().moveToLastPage(context.getEditorCas(),
                        FocusPosition.TOP)));
    }

    private LambdaAjaxLink bindShortcut(String aId, SerializableSupplier<KeyCombo> aCombo,
            PagingAction aAction)
    {
        var link = new LambdaAjaxLink(aId, target -> onActiveEditor(target, aAction));
        link.add(aCombo.get().toInputBehavior(click));
        return link;
    }

    private void onActiveEditor(AjaxRequestTarget aTarget, PagingAction aAction) throws Exception
    {
        var editor = manager.getActiveEditor().orElse(null);

        if (editor == null || !isPageable(editor)) {
            return;
        }

        aAction.apply(aTarget, editor);
        editor.actionRefreshDocument(aTarget);
    }

    private boolean isPageable(DiamContext aContext)
    {
        if (aContext.getAnnotatorState().getDocument() == null) {
            return false;
        }

        var pagingStrategy = aContext.getAnnotatorState().getPagingStrategy();

        return pagingStrategy != null && !(pagingStrategy instanceof NoPagingStrategy);
    }

    @FunctionalInterface
    private interface PagingAction
        extends Serializable
    {
        void apply(AjaxRequestTarget aTarget, DiamContext aContext) throws Exception;
    }
}
