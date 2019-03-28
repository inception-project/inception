/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class DefaultPagingNavigator extends Panel
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
        
        Form<Void> gotoPageTextFieldForm = new Form<>("gotoPageTextFieldForm");
        gotoPageTextField = new NumberTextField<>("gotoPageText", Model.of(1), Integer.class);
        // Using a LambdaModel here because the model object in the page may change and we want to
        // always get the right one
        gotoPageTextField.setModel(
                PropertyModel.of(LambdaModel.of(() -> aPage.getModel()), "firstVisibleUnitIndex"));
        // FIXME minimum and maximum should be obtained from the annotator state
        gotoPageTextField.setMinimum(1);
        //gotoPageTextField.setMaximum(LambdaModel.of(() -> aPage.getModelObject().getUnitCount()));
        gotoPageTextField.setOutputMarkupId(true); 
        gotoPageTextFieldForm.add(gotoPageTextField);
        LambdaAjaxSubmitLink gotoPageLink = new LambdaAjaxSubmitLink("gotoPageLink",
                gotoPageTextFieldForm, this::actionGotoPage);
        gotoPageTextFieldForm.setDefaultButton(gotoPageLink);
        gotoPageTextFieldForm.add(gotoPageLink);
        add(gotoPageTextFieldForm);
        
        add(new ActionBarLink("showNext", t -> actionShowNextPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        add(new ActionBarLink("showPrevious", t -> actionShowPreviousPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new ActionBarLink("showFirst", t -> actionShowFirstPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new ActionBarLink("showLast", t -> actionShowLastPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));
    }
    
    public AnnotatorState getModelObject()
    {
        return page.getModelObject();
    }
    
    protected void actionShowPreviousPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToPreviousPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    protected void actionShowNextPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToNextPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    protected void actionShowFirstPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToFirstPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }

    protected void actionShowLastPage(AjaxRequestTarget aTarget)
        throws Exception
    {
        CAS cas = page.getEditorCas();
        getModelObject().moveToLastPage(cas, defaultFocusPosition);
        page.actionRefreshDocument(aTarget);
    }
    
    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm)
        throws Exception
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
    
    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        aEvent.getRequestHandler().add(gotoPageTextField);
    }

}
