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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public abstract class EditorAjaxRequestHandlerBase
    implements EditorAjaxRequestHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getId()
    {
        return getCommand();
    }

    protected AjaxRequestTarget getAjaxRequestTarget()
    {
        return RequestCycle.get() //
                .find(AjaxRequestTarget.class) //
                .get();
    }

    protected AnnotationPageBase getPage()
    {
        return (AnnotationPageBase) getAjaxRequestTarget().getPage();
    }

    protected AnnotatorState getAnnotatorState()
    {
        return getPage().getModelObject();
    }

    protected String getAction(Request aRequest)
    {
        return aRequest.getRequestParameters().getParameterValue(PARAM_ACTION).toString();
    }

    public VID getVid(Request aRequest)
    {
        IRequestParameters requestParameters = aRequest.getRequestParameters();

        return VID.parseOptional(requestParameters.getParameterValue(PARAM_ID).toString());
    }

    protected DefaultAjaxResponse handleError(String aMessage, Exception e)
    {
        AjaxRequestTarget target = getAjaxRequestTarget();

        target.addChildren(target.getPage(), IFeedback.class);

        String fullMessage = aMessage + ": " + e.getMessage();

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            target.getPage().error(fullMessage);
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error(fullMessage, e);
            }
            return new DefaultAjaxResponse(getAction(RequestCycle.get().getRequest()),
                    asList(fullMessage));
        }

        LOG.error("{}", aMessage, e);
        target.getPage().error(fullMessage);
        return new DefaultAjaxResponse(getAction(RequestCycle.get().getRequest()),
                asList(aMessage));
    }

}
