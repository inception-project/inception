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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.Serializable;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.settings.MergeStrategyPanel;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxFormCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class MergeDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 5194857538069045172L;

    private final ResourceModel titleModel;
    private final ResourceModel challengeModel;
    private final IModel<String> expectedResponseModel;
    private final IModel<CurationWorkflow> curationWorkflowModel;

    private AjaxFormCallback<State> confirmAction;
    private AjaxCallback cancelAction;
    private ContentPanel conentPanel;

    public MergeDialog(String aId, ResourceModel aTitle, ResourceModel aChallenge,
            IModel<String> aExpectedResponse, IModel<CurationWorkflow> aCurationWorkflow)
    {
        super(aId);

        titleModel = aTitle;
        challengeModel = aChallenge;
        expectedResponseModel = aExpectedResponse;
        curationWorkflowModel = aCurationWorkflow;

        setOutputMarkupId(true);

        setModel(new CompoundPropertyModel<>(null));

        setContent(conentPanel = new ContentPanel(CONTENT_ID, getModel()));
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

    public void show(AjaxRequestTarget aTarget)
    {
        challengeModel.detach();
        expectedResponseModel.detach();

        setModelObject(new State());
        aTarget.focusComponent(conentPanel.responseField);

        open(aTarget);
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
        if (!Objects.equals(expectedResponseModel.getObject(), state.response)) {
            info("Your response did not meet the challenge.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var closeOk = true;

        // Invoke callback if one is defined
        if (confirmAction != null) {
            try {
                confirmAction.accept(aTarget, aForm);
            }
            catch (ReplaceHandlerException e) {
                // Let Wicket redirects still work
                throw e;
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                info("Unable to perform merge: " + getRootCauseMessage(e));
                aTarget.addChildren(getPage(), IFeedback.class);
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
                info("Unable to perform merge: " + getRootCauseMessage(e));
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }

        close(aTarget);
    }

    public static class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        String response;
        boolean saveSettingsAsDefault;
        boolean clearTargetCas = true;

        public boolean isSaveSettingsAsDefault()
        {
            return saveSettingsAsDefault;
        }

        public boolean isClearTargetCas()
        {
            return clearTargetCas;
        }
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private final LambdaAjaxLink cancelButton;

        private TextField<Object> responseField;

        public ContentPanel(String aId, IModel<State> aModel)
        {
            super(aId, aModel);

            queue(new Form<>("form", aModel));
            queue(new Label("title", titleModel));
            var challenge = new Label("challenge", challengeModel);
            challenge.setEscapeModelStrings(false); // SAFE - challengeModel is a ResourceModel
            // which can only come from a trusted resource bundle
            queue(challenge);
            queue(new Label("expectedResponse", expectedResponseModel));
            responseField = new TextField<>("response");
            responseField.setOutputMarkupId(true);
            queue(responseField);
            queue(new MergeStrategyPanel("mergeStrategySettings", curationWorkflowModel));
            queue(new CheckBox("saveSettingsAsDefault").setOutputMarkupId(true));
            // On the curation page, the option to (not) clear the target CAS is not (yet) supported
            queue(new CheckBox("clearTargetCas").setOutputMarkupId(true)
                    .add(visibleWhen(() -> getPage() instanceof AnnotationPage)));
            queue(new LambdaAjaxButton<>("confirm", MergeDialog.this::onConfirmInternal)
                    .triggerAfterSubmit());
            queue(new LambdaAjaxLink("closeDialog", MergeDialog.this::onCancelInternal));
            cancelButton = new LambdaAjaxLink("cancel", MergeDialog.this::onCancelInternal);
            cancelButton.setOutputMarkupId(true);
            queue(cancelButton);
        }
    }
}
