/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessageGroup;

public class LogDialog
    extends ModalWindow
{
    private static final long serialVersionUID = -6911254813496835955L;
    
    private boolean closeButtonClicked;
    
    private AjaxCallback onChangeAction;

    public LogDialog(String id)
    {
        this(id, null);
    }

    public LogDialog(String id, IModel<List<LogMessageGroup>> aModel)
    {
        super(id, aModel);
        
        // dialog window to select annotation layer preferences
        setInitialWidth(600);
        setInitialHeight(450);
        setResizable(true);
        setWidthUnit("px");
        setHeightUnit("px");
        setTitle("Log");
        setCssClassName("w_blue w_flex");
        setCloseButtonCallback((target) -> {
            closeButtonClicked = true;
            return true;
        });
    }
    
    public IModel<List<LogMessageGroup>> getModel()
    {
        return (IModel<List<LogMessageGroup>>) getDefaultModel();
    }
    
    public MarkupContainer setModel(IModel<List<LogMessageGroup>> aModel)
    {
        return super.setDefaultModel(aModel);
    }
    
    @Override
    public void show(IPartialPageRequestHandler aTarget)
    {
        closeButtonClicked = false;
        
        setWindowClosedCallback((target) -> {
            if (!closeButtonClicked) {
                onConfirmInternal(target);
            }
        });
        
        IModel<List<LogMessageGroup>> model = getModel();
        
        if (model == null || model.getObject() == null) {
            model = new ListModel<>(Collections.emptyList());
        }
        
        setContent(new LogDialogContent(getContentId(), this, model));
        
        super.show(aTarget);
    }

    public AjaxCallback getOnChangeAction()
    {
        return onChangeAction;
    }

    public void setOnChangeAction(AjaxCallback aOnChangeAction)
    {
        onChangeAction = aOnChangeAction;
    }

    protected void onConfirmInternal(AjaxRequestTarget aTarget)
    {
        boolean closeOk = true;
        
        // Invoke callback if one is defined
        if (onChangeAction != null) {
            try {
                onChangeAction.accept(aTarget);
            }
            catch (Exception e) {
                // LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(),
                // e);
                // state.feedback = "Error: " + e.getMessage();
                // aTarget.add(getContent());
                closeOk = false;
            }
        }
        
        if (closeOk) {
            close(aTarget);
        }
    }    
}
