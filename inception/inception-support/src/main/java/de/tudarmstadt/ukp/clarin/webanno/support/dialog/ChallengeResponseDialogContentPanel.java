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
package de.tudarmstadt.ukp.clarin.webanno.support.dialog;

import java.io.Serializable;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class ChallengeResponseDialogContentPanel
    extends Panel
{
    private static final long serialVersionUID = 5202661827792148838L;

    private Form<State> form;
    private Label title;
    private Label message;
    private Label expectedResponse;
    private TextField<String> responseField;
    private LambdaAjaxLink cancel;

    private StringResourceModel titleModel;
    private StringResourceModel messageModel;
    private IModel<String> expectedResponseModel;

    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    public ChallengeResponseDialogContentPanel(String aId)
    {
        super(aId);

        form = new Form<>("form", CompoundPropertyModel.of(new State()));

        title = new Label("title", titleModel);

        message = new Label("message", messageModel);
        message.setEscapeModelStrings(false);

        expectedResponse = new Label("expectedResponse", expectedResponseModel);

        responseField = new TextField<>("response");

        cancel = new LambdaAjaxLink("cancel", this::onCancelInternal);
        cancel.setOutputMarkupId(true);

        queue(new Label("feedback"));
        queue(new LambdaAjaxButton<>("confirm", this::onConfirmInternal));
        queue(new LambdaAjaxLink("closeDialog", this::onCancelInternal));
        queue(title, message, expectedResponse, responseField, cancel, form);
    }

    public void onShow(AjaxRequestTarget aTarget)
    {
        title.setDefaultModel(titleModel);
        message.setDefaultModel(messageModel);
        expectedResponse.setDefaultModel(expectedResponseModel);

        State state = new State();
        state.feedback = null;
        form.setModelObject(state);

        aTarget.focusComponent(cancel);
    }

    protected void onConfirmInternal(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        State state = aForm.getModelObject();

        // Check if the challenge was met
        if (!Objects.equals(expectedResponseModel.getObject(), state.response)) {
            state.feedback = "Your response did not meet the challenge.";
            aTarget.add(aForm);
            return;
        }

        boolean closeOk = true;

        // Invoke callback if one is defined
        if (confirmAction != null) {
            try {
                confirmAction.accept(aTarget);
            }
            catch (ReplaceHandlerException e) {
                // Let Wicket redirects still work
                throw e;
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                state.feedback = "Error: " + e.getMessage();
                aTarget.add(aForm);
                closeOk = false;
            }
        }

        if (closeOk) {
            findParent(ModalDialog.class).close(aTarget);
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
                form.getModelObject().feedback = "Error: " + e.getMessage();
            }
        }

        findParent(ModalDialog.class).close(aTarget);
    }

    public StringResourceModel getTitleModel()
    {
        return titleModel;
    }

    public void setTitleModel(StringResourceModel aTitleModel)
    {
        titleModel = aTitleModel;
    }

    public void setMessageModel(StringResourceModel aMessageModel)
    {
        messageModel = aMessageModel;
    }

    public StringResourceModel getMessageModel()
    {
        return messageModel;
    }

    public void setExpectedResponseModel(IModel<String> aExpectedResponseModel)
    {
        expectedResponseModel = aExpectedResponseModel;
    }

    public IModel<String> getExpectedResponseModel()
    {
        return expectedResponseModel;
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

    private class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        private String response;
        @SuppressWarnings("unused")
        private String feedback;
    }
}
