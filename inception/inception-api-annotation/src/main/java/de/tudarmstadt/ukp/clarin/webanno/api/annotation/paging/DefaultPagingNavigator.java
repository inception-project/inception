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

import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyBindingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.KeyCombo;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class DefaultPagingNavigator
    extends Panel
{
    private static final long serialVersionUID = -6315861062996783626L;

    private @SpringBean KeyBindingsProperties keyBindings;

    private final DiamContext editorContext;
    private NumberTextField<Integer> gotoPageTextField;
    private FocusPosition defaultFocusPosition = FocusPosition.TOP;

    public DefaultPagingNavigator(String aId, DiamContext aEditor)
    {
        super(aId);

        Validate.notNull(aEditor, "DiamContext must be set");

        setOutputMarkupPlaceholderTag(true);

        editorContext = aEditor;

        Form<Void> form = new Form<>("form");
        gotoPageTextField = new NumberTextField<>("gotoPageText", Model.of(1), Integer.class);
        // Using a LambdaModel here because the annotator state may change and we want to always get
        // the right one
        gotoPageTextField.setModel(PropertyModel.of(
                LoadableDetachableModel.of(() -> editorContext.getAnnotatorState()),
                "firstVisibleUnitIndex"));
        // FIXME minimum and maximum should be obtained from the annotator state
        gotoPageTextField.setMinimum(1);
        // gotoPageTextField.setMaximum(LambdaModel.of(() ->
        // aPage.getModelObject().getUnitCount()));
        gotoPageTextField.setOutputMarkupId(true);
        form.add(gotoPageTextField);
        var gotoPageLink = new LambdaAjaxSubmitLink<>("gotoPageLink", form, this::actionGotoPage);
        form.setDefaultButton(gotoPageLink);
        form.add(gotoPageLink);
        add(form);

        form.add(withShortcutHint(new LambdaAjaxLink("showNext", this::actionShowNextPage),
                () -> keyBindings.getNavigation().getNextPage()));
        form.add(withShortcutHint(new LambdaAjaxLink("showPrevious", this::actionShowPreviousPage),
                () -> keyBindings.getNavigation().getPreviousPage()));
        form.add(withShortcutHint(new LambdaAjaxLink("showFirst", this::actionShowFirstPage),
                () -> keyBindings.getNavigation().getFirstPage()));
        form.add(withShortcutHint(new LambdaAjaxLink("showLast", this::actionShowLastPage),
                () -> keyBindings.getNavigation().getLastPage()));

        form.add(LambdaBehavior.visibleWhen(() -> !contentFitsFullyIntoVisibleWindow()));
    }

    /**
     * Advertise the page-wide keyboard shortcut in a button's tooltip.
     * <p>
     * Only the hint lives here - the binding itself belongs to {@code PagingKeyBindingsPanel},
     * because a shortcut is a page-wide resource while this navigator is per-editor. The two agree
     * on the action but not on the target: the shortcut moves whichever editor is active, this
     * button always moves its own.
     */
    private LambdaAjaxLink withShortcutHint(LambdaAjaxLink aLink,
            SerializableSupplier<KeyCombo> aCombo)
    {
        aLink.add(AttributeModifier.append("title",
                () -> " (" + KeyBindingsUtil.formatShortcut(aCombo.get()) + ")"));
        return aLink;
    }

    private boolean contentFitsFullyIntoVisibleWindow()
    {
        var state = editorContext.getViewState();
        return state.getUnitCount() <= state.getPreferences().getWindowSize();
    }

    protected void actionShowPreviousPage(AjaxRequestTarget aTarget) throws Exception
    {
        var cas = editorContext.getEditorCas();
        editorContext.getViewState().moveToPreviousPage(cas, defaultFocusPosition);
        editorContext.actionRefreshDocument(aTarget);
    }

    protected void actionShowNextPage(AjaxRequestTarget aTarget) throws Exception
    {
        var cas = editorContext.getEditorCas();
        editorContext.getViewState().moveToNextPage(cas, defaultFocusPosition);
        editorContext.actionRefreshDocument(aTarget);
    }

    protected void actionShowFirstPage(AjaxRequestTarget aTarget) throws Exception
    {
        var cas = editorContext.getEditorCas();
        editorContext.getViewState().moveToFirstPage(cas, defaultFocusPosition);
        editorContext.actionRefreshDocument(aTarget);
    }

    protected void actionShowLastPage(AjaxRequestTarget aTarget) throws Exception
    {
        var cas = editorContext.getEditorCas();
        editorContext.getViewState().moveToLastPage(cas, defaultFocusPosition);
        editorContext.actionRefreshDocument(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm) throws Exception
    {
        var cas = editorContext.getEditorCas();
        editorContext.getViewState().moveToUnit(cas, gotoPageTextField.getModelObject(),
                defaultFocusPosition);
        editorContext.actionRefreshDocument(aTarget);
    }

    public void setDefaultFocusPosition(FocusPosition aPos)
    {
        defaultFocusPosition = aPos;
    }

    public FocusPosition getDefaultFocusPosition()
    {
        return defaultFocusPosition;
    }

    /**
     * Re-render the current position if the position has been changed in the {@link AnnotatorState}
     */
    @OnEvent
    public void onAnnotatorViewStateChangedEvent(AnnotatorViewportChangedEvent aEvent)
    {
        // Only react to viewport changes in the editor this navigator belongs to (#6146).
        if (!aEvent.isFor(editorContext.getAnnotatorState())) {
            return;
        }

        aEvent.getRequestHandler().add(gotoPageTextField);
    }
}
