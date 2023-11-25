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
package de.tudarmstadt.ukp.inception.support.lambda;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

public class LambdaAjaxFormSubmittingBehavior
    extends AjaxFormSubmitBehavior
{
    private static final long serialVersionUID = 3946442967075930557L;

    private AjaxCallback action;
    private AjaxExceptionHandler exceptionHandler;

    /**
     * @param aForm
     *            form that will be submitted
     * @param aEvent
     *            javascript event this behavior is attached to, like onclick
     * @param aAction
     *            the action to perform when the form is submitted
     */
    public LambdaAjaxFormSubmittingBehavior(Form<?> aForm, String aEvent, AjaxCallback aAction)
    {
        this(aForm, aEvent, aAction, null);
    }

    /**
     * @param aForm
     *            form that will be submitted
     * @param aEvent
     *            javascript event this behavior is attached to, like onclick
     * @param aAction
     *            the action to perform when the form is submitted
     * @param aExceptionHandler
     *            an exception handler to call if there was a problem submitting the form
     */
    public LambdaAjaxFormSubmittingBehavior(Form<?> aForm, String aEvent, AjaxCallback aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aForm, aEvent);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    public LambdaAjaxFormSubmittingBehavior(String aId)
    {
        this(aId, null, null);
    }

    public LambdaAjaxFormSubmittingBehavior(String aId, AjaxCallback aAction)
    {
        this(aId, aAction, null);
    }

    public LambdaAjaxFormSubmittingBehavior(String aId, AjaxCallback aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aId);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    @Override
    public void onSubmit(AjaxRequestTarget aTarget)
    {
        try {
            if (action != null) {
                action.accept(aTarget);
            }
        }
        catch (ReplaceHandlerException e) {
            // Let Wicket redirects still work
            throw e;
        }
        catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(aTarget, e);
            }
            else {
                Component component = getComponent();
                LoggerFactory.getLogger(component.getPage().getClass())
                        .error("Error: " + e.getMessage(), e);
                component.error("Error: " + e.getMessage());
                aTarget.addChildren(component.getPage(), IFeedback.class);
            }
        }
    }
}
