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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

public class LambdaAjaxLink
    extends AjaxLink<Void>
{
    private static final long serialVersionUID = 3946442967075930557L;

    private AjaxCallback action;
    private AjaxExceptionHandler exceptionHandler;
    private SerializableMethodDelegate<LambdaAjaxLink> onConfigureAction;
    private boolean alwaysEnabled;

    public LambdaAjaxLink(String aId, AjaxCallback aAction)
    {
        this(aId, aAction, null);
    }

    public LambdaAjaxLink(String aId, AjaxCallback aAction, AjaxExceptionHandler aExceptionHandler)
    {
        super(aId);
        action = aAction;
        exceptionHandler = aExceptionHandler;
        add(AttributeAppender.append("class", () -> isEnabledInHierarchy() ? "" : "disabled"));
    }

    public LambdaAjaxLink onConfigure(SerializableMethodDelegate<LambdaAjaxLink> aAction)
    {
        onConfigureAction = aAction;
        return this;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        if (onConfigureAction != null) {
            onConfigureAction.run(this);
        }
    }

    @Override
    public void onClick(AjaxRequestTarget aTarget)
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
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
    }

    public LambdaAjaxLink setAlwaysEnabled(boolean aAlwaysEnabled)
    {
        alwaysEnabled = aAlwaysEnabled;
        return this;
    }

    @Override
    public boolean isEnabledInHierarchy()
    {
        return alwaysEnabled || super.isEnabledInHierarchy();
    }
}
