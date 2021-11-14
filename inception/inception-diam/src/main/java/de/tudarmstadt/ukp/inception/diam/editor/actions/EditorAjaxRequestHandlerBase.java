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

import java.lang.invoke.MethodHandles;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;

public abstract class EditorAjaxRequestHandlerBase
    implements EditorAjaxRequestHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getId()
    {
        return getCommand();
    }

    protected void handleError(AjaxRequestTarget aTarget, String aMessage, Exception e)
    {
        aTarget.addChildren(aTarget.getPage(), IFeedback.class);

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            aTarget.getPage().error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;
        }

        LOG.error("{}", aMessage, e);
        aTarget.getPage().error(aMessage);
    }

}
