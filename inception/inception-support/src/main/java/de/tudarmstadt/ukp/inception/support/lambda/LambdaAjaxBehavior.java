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

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

public class LambdaAjaxBehavior
    extends AbstractDefaultAjaxBehavior
{
    private static final long serialVersionUID = 4211352559572747320L;

    private AjaxCallback action;
    private AjaxExceptionHandler exceptionHandler;
    private boolean preventDefault = false;

    public LambdaAjaxBehavior()
    {
        this(null, null);
    }

    public LambdaAjaxBehavior(AjaxCallback aAction)
    {
        this(aAction, null);
    }

    public LambdaAjaxBehavior(AjaxCallback aAction, AjaxExceptionHandler aExceptionHandler)
    {
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    public void setAction(AjaxCallback aAction)
    {
        action = aAction;
    }

    public void setExceptionHandler(AjaxExceptionHandler aExceptionHandler)
    {
        exceptionHandler = aExceptionHandler;
    }

    @Override
    protected void respond(AjaxRequestTarget aTarget)
    {
        if (action == null) {
            return;
        }

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

    public LambdaAjaxBehavior setPreventDefault(boolean aPreventDefault)
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
