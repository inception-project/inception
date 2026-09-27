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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.End;

import java.io.Serializable;
import java.util.Optional;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.visit.IVisit;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class ActionBarKeyBindingsPanel
    extends Panel
{
    private static final long serialVersionUID = -2146018384462427195L;

    private @SpringBean KeyBindingsProperties keyBindings;

    private final DocumentEditorManager manager;

    public ActionBarKeyBindingsPanel(String aId, DocumentEditorManager aManager)
    {
        super(aId);

        manager = aManager;

        setOutputMarkupId(true);

        add(bindShortcut("showPreviousDocument",
                keyBindings.getNavigation().getPreviousDocument().toInputBehavior(click),
                DocumentNavigationHandler.class,
                DocumentNavigationHandler::actionShowPreviousDocument));

        add(bindShortcut("showNextDocument",
                keyBindings.getNavigation().getNextDocument().toInputBehavior(click),
                DocumentNavigationHandler.class,
                DocumentNavigationHandler::actionShowNextDocument));

        add(bindShortcut("finishOrReopenDocument",
                new InputBehavior(new KeyType[] { Ctrl, End }, click), DocumentStateHandler.class,
                DocumentStateHandler::actionFinishOrReopenDocument));
    }

    private <T> LambdaAjaxLink bindShortcut(String aId, Behavior aShortcut, Class<T> aHandlerType,
            HandlerAction<T> aAction)
    {
        var link = new LambdaAjaxLink(aId, target -> onActiveEditor(target, aHandlerType, aAction));
        link.add(aShortcut);
        return link;
    }

    private <T> void onActiveEditor(AjaxRequestTarget aTarget, Class<T> aHandlerType,
            HandlerAction<T> aAction)
        throws Exception
    {
        var handler = manager.getActiveEditor() //
                .flatMap(editor -> findHandler(editor, aHandlerType)) //
                .orElse(null);

        if (handler == null) {
            return;
        }

        aAction.apply(handler, aTarget);
    }

    private static <T> Optional<T> findHandler(DocumentEditor aEditor, Class<T> aHandlerType)
    {
        if (!(aEditor instanceof MarkupContainer container)) {
            return Optional.empty();
        }

        return Optional.ofNullable(container.visitChildren(ActionBar.class, (ActionBar actionBar,
                IVisit<T> visit) -> actionBar.findItem(aHandlerType).ifPresent(visit::stop)));
    }

    @FunctionalInterface
    private interface HandlerAction<T>
        extends Serializable
    {
        void apply(T aHandler, AjaxRequestTarget aTarget) throws Exception;
    }
}
