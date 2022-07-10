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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class ConfirmationDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 5194857538069045172L;

    private IModel<String> titleModel;
    private IModel<String> contentModel;

    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    private ContentPanel contentPanel;

    public ConfirmationDialog(String aId)
    {
        this(aId, null, null);
        titleModel = new StringResourceModel("title", this, null);
        contentModel = new StringResourceModel("text", this, null);
    }

    public ConfirmationDialog(String aId, IModel<String> aTitle)
    {
        this(aId, aTitle, Model.of());
    }

    public ConfirmationDialog(String aId, IModel<String> aTitle, IModel<String> aContent)
    {
        super(aId);

        titleModel = aTitle;
        contentModel = aContent;
        add(new DefaultTheme());

        setOutputMarkupId(true);

        trapFocus();
        // closeOnEscape();
        // closeOnClick();

        setModel(new CompoundPropertyModel<>(null));

        contentPanel = new ContentPanel(ModalDialog.CONTENT_ID, getModel());
        setContent(contentPanel);
    }

    public IModel<String> getTitleModel()
    {
        return titleModel;
    }

    public void setTitleModel(IModel<String> aTitleModel)
    {
        titleModel = aTitleModel;
    }

    public IModel<String> getContentModel()
    {
        return contentModel;
    }

    public void setContentModel(IModel<String> aContentModel)
    {
        contentModel = aContentModel;
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
        contentModel.detach();

        State state = new State();
        state.feedback = null;
        setModelObject(state);

        open(aTarget);

        aTarget.focusComponent(contentPanel.cancel);
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

    private class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        private String feedback;
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private Form<State> form;
        private LambdaAjaxLink cancel;

        public ContentPanel(String aId, IModel<State> aModel)
        {
            super(aId, aModel);

            form = new Form<>("form", CompoundPropertyModel.of(aModel));
            form.add(new Label("title", ConfirmationDialog.this.titleModel));
            form.add(new Label("content", ConfirmationDialog.this.contentModel)
                    .setEscapeModelStrings(false));
            form.add(new Label("feedback"));
            form.add(new LambdaAjaxButton<>("confirm", ConfirmationDialog.this::onConfirmInternal));
            form.add(cancel = new LambdaAjaxLink("cancel",
                    ConfirmationDialog.this::onCancelInternal));
            form.add(new LambdaAjaxLink("closeDialog", ConfirmationDialog.this::onCancelInternal));

            add(form);
        }
    }
}
