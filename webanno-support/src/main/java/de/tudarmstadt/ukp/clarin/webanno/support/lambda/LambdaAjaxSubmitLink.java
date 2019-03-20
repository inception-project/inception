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
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
public class LambdaAjaxSubmitLink
    extends AjaxSubmitLink
{
    private static final long serialVersionUID = 3946442967075930557L;

    private AjaxFormCallback action;
    private AjaxExceptionHandler exceptionHandler;

    public LambdaAjaxSubmitLink(String aId, AjaxFormCallback<?> aAction)
    {
        this(aId, aAction, null);
    }

    public LambdaAjaxSubmitLink(String aId, AjaxFormCallback<?> aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aId);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    public LambdaAjaxSubmitLink(String aId, Form aForm, AjaxFormCallback<?> aAction)
    {
        this(aId, aForm, aAction, null);
    }

    public LambdaAjaxSubmitLink(String aId, Form aForm, AjaxFormCallback<?> aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aId, aForm);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    @Override
    protected void onSubmit(AjaxRequestTarget aTarget)
    {
        try {
            action.accept(aTarget, getForm());
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
}
