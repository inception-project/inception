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
package de.tudarmstadt.ukp.clarin.webanno.support.lambda;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.slf4j.LoggerFactory;

public class LambdaAjaxButton<T>
    extends AjaxButton
{
    private static final long serialVersionUID = 3946442967075930557L;

    private AjaxFormCallback<T> action;
    private AjaxExceptionHandler exceptionHandler;
    private boolean triggerAfterSubmit;

    public LambdaAjaxButton(String aId, AjaxFormCallback<T> aAction)
    {
        this(aId, aAction, null);
    }

    public LambdaAjaxButton(String aId, AjaxFormCallback<T> aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aId);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    public LambdaAjaxButton<T> triggerAfterSubmit()
    {
        triggerAfterSubmit = true;
        return this;
    }

    @Override
    public void onSubmit(AjaxRequestTarget aTarget)
    {
        if (!triggerAfterSubmit) {
            action(aTarget);
        }
    }
    
    @Override
    public void onAfterSubmit(AjaxRequestTarget aTarget)
    {
        if (triggerAfterSubmit) {
            action(aTarget);
        }
    }
    
    
    @SuppressWarnings("unchecked")
    private void action(AjaxRequestTarget aTarget)
    {
        try {
            action.accept(aTarget, (Form<T>) getForm());
        }
        catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(aTarget, e);
            }
            else {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
    }
    
    @Override
    protected void onError(AjaxRequestTarget aTarget)
    {
        super.onError(aTarget);
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
