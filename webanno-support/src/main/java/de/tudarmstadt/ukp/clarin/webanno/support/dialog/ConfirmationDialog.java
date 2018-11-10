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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class ConfirmationDialog
    extends ModalWindow
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
    
    @Override
    public void show(IPartialPageRequestHandler aTarget)
    {
        contentModel.detach();
        
        State state = new State();
        state.content = contentModel.getObject();
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

        private String content;
        private String feedback;
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private Form<State> form;

        public ContentPanel(String aId, IModel<State> aModel)
        {
            super(aId, aModel);

            form = new Form<>("form", CompoundPropertyModel.of(aModel));
            form.add(new Label("content").setEscapeModelStrings(false));
            form.add(new Label("feedback"));
            form.add(new LambdaAjaxButton<>("confirm", ConfirmationDialog.this::onConfirmInternal));
            form.add(new LambdaAjaxLink("cancel", ConfirmationDialog.this::onCancelInternal));
            
            add(form);
        }
    }
}
