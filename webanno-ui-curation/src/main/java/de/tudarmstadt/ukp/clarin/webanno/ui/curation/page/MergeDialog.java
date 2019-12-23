/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import java.io.Serializable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxFormCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class MergeDialog
    extends ModalWindow
{
    private static final long serialVersionUID = 5194857538069045172L;

    private IModel<String> titleModel;
    private IModel<String> challengeModel;
    private IModel<String> expectedResponseModel;
    
    private AjaxFormCallback<State> confirmAction;
    private AjaxCallback cancelAction;

    private ContentPanel contentPanel;
    
    public MergeDialog(String aId, IModel<String> aTitle, IModel<String> aChallenge,
            IModel<String> aExpectedResponse)
    {
        super(aId);
        
        titleModel = aTitle;
        challengeModel = aChallenge;
        expectedResponseModel = aExpectedResponse;
        
        setOutputMarkupId(true);
        setInitialWidth(620);
        setInitialHeight(440);
        setResizable(true);
        setWidthUnit("px");
        setHeightUnit("px");
        setCssClassName("w_blue w_flex");
        showUnloadConfirmation(false);
        
        setModel(new CompoundPropertyModel<>(null));
        
        setContent(contentPanel = new ContentPanel(getContentId(), getModel()));
        
        setCloseButtonCallback((_target) -> {
            onCancelInternal(_target);
            return true;
        });
    }
    
    public void setModel(IModel<State> aModel)
    {
        setDefaultModel(aModel);
    }
    
    @SuppressWarnings("unchecked")
    public IModel<State> getModel()
    {
        return (IModel<State>) getDefaultModel();
    }

    public void setModelObject(State aModel)
    {
        setDefaultModelObject(aModel);
    }
    
    public State getModelObject()
    {
        return (State) getDefaultModelObject();
    }    
    
    
    
    @Override
    public void show(IPartialPageRequestHandler aTarget)
    {
        challengeModel.detach();
        expectedResponseModel.detach();
        
        State state = new State();
        state.challenge = challengeModel.getObject();
        state.expectedResponse = expectedResponseModel.getObject();
        state.response = null;
        state.feedback = null;
        setModelObject(state);

        setTitle(titleModel.getObject());
        
        super.show(aTarget);
    }
    
    public AjaxFormCallback<State> getConfirmAction()
    {
        return confirmAction;
    }

    public void setConfirmAction(AjaxFormCallback<State> aConfirmAction)
    {
        confirmAction = aConfirmAction;
    }

    public AjaxCallback getCancelAction()
    {
        return cancelAction;
    }

    public void setCancelAction(AjaxCallback aCancelAction)
    {
        cancelAction = aCancelAction;
    }

    protected void onConfirmInternal(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        State state = aForm.getModelObject();
        
        // Check if the challenge was met
        if (!ObjectUtils.equals(state.expectedResponse, state.response)) {
            state.feedback = "Your response did not meet the challenge.";
            aTarget.add(aForm);
            return;
        }
        
        boolean closeOk = true;
        
        // Invoke callback if one is defined
        if (confirmAction != null) {
            try {
                confirmAction.accept(aTarget, aForm);
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                state.feedback = "Error: " + e.getMessage();
                aTarget.add(aForm);
                closeOk = false;
            }
        }
        
        if (closeOk) {
            close(aTarget);
        }
    }

    protected void onCancelInternal(AjaxRequestTarget aTarget)
    {
        if (cancelAction != null) {
            try {
                cancelAction.accept(aTarget);
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                contentPanel.form.getModelObject().feedback = "Error: " + e.getMessage();
            }
        }
        close(aTarget);
    }

    public static class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        String challenge;
        String expectedResponse;
        String response;
        String feedback;
        boolean mergeIncompleteAnnotations;
        
        public boolean isMergeIncompleteAnnotations()
        {
            return mergeIncompleteAnnotations;
        }
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private FeedbackPanel feedbackPanel;
        private Form<State> form;

        public ContentPanel(String aId, IModel<State> aModel)
        {
            super(aId, aModel);

            form = new Form<>("form", aModel);
            form.add(new Label("challenge").setEscapeModelStrings(false));
            form.add(new Label("feedback"));
            form.add(new TextField<>("response"));
            form.add(new CheckBox("mergeIncompleteAnnotations"));
            form.add(new LambdaAjaxButton<>("confirm",
                    MergeDialog.this::onConfirmInternal));
            form.add(new LambdaAjaxLink("cancel",
                    MergeDialog.this::onCancelInternal));
            
            add(form);
        }
    }
}
