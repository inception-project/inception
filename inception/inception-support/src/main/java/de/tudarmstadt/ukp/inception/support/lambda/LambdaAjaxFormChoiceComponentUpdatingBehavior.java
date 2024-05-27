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
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

public class LambdaAjaxFormChoiceComponentUpdatingBehavior
    extends AjaxFormChoiceComponentUpdatingBehavior
{
    private static final long serialVersionUID = -8496566485055940375L;

    private AjaxCallback action;
    private AjaxExceptionHandler exceptionHandler;

    public LambdaAjaxFormChoiceComponentUpdatingBehavior()
    {
        this(null, null);
    }

    public LambdaAjaxFormChoiceComponentUpdatingBehavior(AjaxCallback aAction)
    {
        this(aAction, null);
    }

    public LambdaAjaxFormChoiceComponentUpdatingBehavior(AjaxCallback aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super();
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    @Override
    public void onUpdate(AjaxRequestTarget aTarget)
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
