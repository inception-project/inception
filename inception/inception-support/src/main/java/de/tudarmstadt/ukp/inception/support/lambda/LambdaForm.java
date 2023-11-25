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
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.LoggerFactory;

public class LambdaForm<T>
    extends Form<T>
{
    private static final long serialVersionUID = -6569358514293674347L;

    AjaxFormCallback<T> submitAction;

    public LambdaForm(String aId, IModel<T> aModel)
    {
        super(aId, aModel);
    }

    public LambdaForm(String aId)
    {
        super(aId);
    }

    public LambdaForm<T> onSubmit(AjaxFormCallback<T> aCallback)
    {
        submitAction = aCallback;
        return this;
    }

    @Override
    protected void onSubmit()
    {
        if (submitAction == null) {
            return;
        }

        try {
            submitAction.accept(RequestCycle.get().find(AjaxRequestTarget.class).orElse(null),
                    this);
        }
        catch (ReplaceHandlerException e) {
            // Let Wicket redirects still work
            throw e;
        }
        catch (Exception e) {
            LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
            error("Error: " + e.getMessage());
            RequestCycle.get().find(AjaxRequestTarget.class)
                    .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }
    }
}
