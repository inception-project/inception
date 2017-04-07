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
package de.tudarmstadt.ukp.clarin.webanno.support.dialog;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;

public class ConfirmationDialog
    extends ModalWindow
{
    private static final long serialVersionUID = 5194857538069045172L;

    private IModel<String> titleModel;
    private IModel<String> challengeModel;
    
    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    public ConfirmationDialog(String aId, IModel<String> aTitle, IModel<String> aChallenge)
    {
        super(aId);
        
        titleModel = aTitle;
        challengeModel = aChallenge;
        
        setOutputMarkupId(true);
        setInitialWidth(620);
        setInitialHeight(440);
        setResizable(true);
        setWidthUnit("px");
        setHeightUnit("px");
        showUnloadConfirmation(false);
        
        setModel(new CompoundPropertyModel<>(null));
        
        setContent(new ContentPanel(getContentId(), getModel()));
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
        
        State state = new State();
        state.challenge = challengeModel.getObject();
        state.feedback = null;
        setModelObject(state);

        setTitle(titleModel.getObject());
        
        super.show(aTarget);
    }
    
    public AjaxCallback getConfirmAction()
    {
        return confirmAction;
    }

    public void setConfirmAction(AjaxCallback aConfirmAction)
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
        
        boolean closeOk = true;
        
        // Invoke callback if one is defined
        if (confirmAction != null) {
            try {
                confirmAction.accept(aTarget);
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

    protected void onCancelInternal(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        if (cancelAction != null) {
            try {
                cancelAction.accept(aTarget);
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                aForm.getModelObject().feedback = "Error: " + e.getMessage();
            }
        }
        close(aTarget);
    }

    private class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        private String challenge;
        private String expectedResponse;
        private String feedback;
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private FeedbackPanel feedbackPanel;

        public ContentPanel(String aId, IModel<State> aModel)
        {
            super(aId, aModel);

            Form<State> form = new Form<>("form", aModel);
            form.add(new Label("challenge").setEscapeModelStrings(false));
            form.add(new Label("feedback"));
            form.add(new LambdaAjaxButton<State>("confirm",  ConfirmationDialog.this::onConfirmInternal));
            form.add(new LambdaAjaxButton<State>("cancel", ConfirmationDialog.this::onCancelInternal));
            
            add(form);
        }
    }
}
