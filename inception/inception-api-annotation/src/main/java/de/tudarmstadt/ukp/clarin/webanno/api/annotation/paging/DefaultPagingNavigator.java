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

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.key.KeyType;

public class DefaultPagingNavigator
    extends Panel
{
    private static final long serialVersionUID = -6315861062996783626L;

    private AnnotationPageBase page;
    private NumberTextField<Integer> gotoPageTextField;
    private FocusPosition defaultFocusPosition = FocusPosition.TOP;

    public DefaultPagingNavigator(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        setOutputMarkupPlaceholderTag(true);

        page = aPage;

        Form<Void> form = new Form<>("form");
        gotoPageTextField = new NumberTextField<>("gotoPageText", Model.of(1), Integer.class);
        // Using a LambdaModel here because the model object in the page may change and we want to
        // always get the right one
        gotoPageTextField.setModel(PropertyModel
                .of(LoadableDetachableModel.of(() -> aPage.getModel()), "firstVisibleUnitIndex"));
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

        form.add(new LambdaAjaxLink("showNext", t -> actionShowNextPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        form.add(new LambdaAjaxLink("showPrevious", t -> actionShowPreviousPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        form.add(new LambdaAjaxLink("showFirst", t -> actionShowFirstPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        form.add(new LambdaAjaxLink("showLast", t -> actionShowLastPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        form.add(LambdaBehavior.visibleWhen(() -> !contentFitsFullyIntoVisibleWindow()));
    }

    private boolean contentFitsFullyIntoVisibleWindow()
    {
        AnnotatorState state = page.getModelObject();
        return state.getUnitCount() <= state.getPreferences().getWindowSize();
    }

    public AnnotatorState getModelObject()
    {
        return page.getModelObject();
    }

    protected void actionShowPreviousPage(AjaxRequestTarget aTarget) throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToPreviousPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    protected void actionShowNextPage(AjaxRequestTarget aTarget) throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToNextPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    protected void actionShowFirstPage(AjaxRequestTarget aTarget) throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToFirstPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    protected void actionShowLastPage(AjaxRequestTarget aTarget) throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToLastPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm) throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToUnit(cas, gotoPageTextField.getModelObject(), defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
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
        aEvent.getRequestHandler().add(gotoPageTextField);
    }
}
