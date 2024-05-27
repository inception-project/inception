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
package de.tudarmstadt.ukp.inception.bootstrap.dialog;

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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public abstract class ChallengeResponseDialogContentPanel_ImplBase
    extends Panel
{
    private static final long serialVersionUID = -4504578209205451192L;

    private Form<State> form;
    private Label title;
    private Label expectedResponse;
    private TextField<String> responseField;
    private LambdaAjaxButton<State> confirm;
    private LambdaAjaxLink cancel;

    private ResourceModel titleModel;
    private IModel<String> expectedResponseModel;

    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    public ChallengeResponseDialogContentPanel_ImplBase(String aId)
    {
        this(aId, null);
    }

    public ChallengeResponseDialogContentPanel_ImplBase(String aId, ResourceModel aTitleModel)
    {
        super(aId);

        form = new Form<>("form", CompoundPropertyModel.of(new State()));

        titleModel = aTitleModel;
        title = new Label("title", titleModel);

        expectedResponse = new Label("expectedResponse", expectedResponseModel);

        responseField = new TextField<>("response");

        cancel = new LambdaAjaxLink("cancel", this::onCancelInternal);
        cancel.setOutputMarkupId(true);

        queue(new Label("feedback"));
        queue(confirm = new LambdaAjaxButton<>("confirm", this::onConfirmInternal));
        queue(new LambdaAjaxLink("closeDialog", this::onCancelInternal));
        queue(title, expectedResponse, responseField, cancel, form);
    }

    public LambdaAjaxButton<State> getConfirmButton()
    {
        return confirm;
    }

    public TextField<String> getResponseField()
    {
        return responseField;
    }

    @Deprecated
    public void onShow(AjaxRequestTarget aTarget)
    {
        title.setDefaultModel(titleModel);
        expectedResponse.setDefaultModel(expectedResponseModel);

        form.setModelObject(new State());

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

    @Deprecated
    public void setTitleModel(ResourceModel aTitleModel)
    {
        titleModel = aTitleModel;
    }

    public void setExpectedResponseModel(IModel<String> aExpectedResponseModel)
    {
        expectedResponseModel = aExpectedResponseModel;
        expectedResponse.setDefaultModel(expectedResponseModel);
    }

    public IModel<String> getExpectedResponseModel()
    {
        return expectedResponseModel;
    }

    public void setConfirmAction(AjaxCallback aConfirmAction)
    {
        confirmAction = aConfirmAction;
    }

    public void setCancelAction(AjaxCallback aCancelAction)
    {
        cancelAction = aCancelAction;
    }

    protected class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        private String response;
        @SuppressWarnings("unused")
        private String feedback;
    }
}
