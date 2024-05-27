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

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

public class LambdaAjaxEventBehavior
    extends AjaxEventBehavior
{
    private static final long serialVersionUID = 4201623024300267614L;

    private AjaxCallback action;
    private AjaxExceptionHandler exceptionHandler;
    private boolean preventDefault = false;

    public LambdaAjaxEventBehavior(String aEvent, AjaxCallback aAction)
    {
        this(aEvent, aAction, null);
    }

    public LambdaAjaxEventBehavior(String aEvent, AjaxCallback aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aEvent);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    @Override
    public void onEvent(AjaxRequestTarget aTarget)
    {
        try {
            action.accept(aTarget);
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
                LoggerFactory.getLogger(getComponent().getPage().getClass())
                        .error("Error: " + e.getMessage(), e);
                getComponent().error("Error: " + e.getMessage());
                aTarget.addChildren(getComponent().getPage(), IFeedback.class);
            }
        }
    }

    public LambdaAjaxEventBehavior setPreventDefault(boolean aPreventDefault)
    {
        preventDefault = aPreventDefault;
        return this;
    }

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
    {
        super.updateAjaxAttributes(aAttributes);
        aAttributes.setPreventDefault(preventDefault);
    };
}
