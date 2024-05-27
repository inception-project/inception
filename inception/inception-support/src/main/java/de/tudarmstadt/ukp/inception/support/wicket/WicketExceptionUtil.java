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
package de.tudarmstadt.ukp.inception.support.wicket;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.feedback.IFeedbackContributor;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.Logger;

public final class WicketExceptionUtil
{
    public static void handleException(Logger aLog, IFeedbackContributor aFeedbackTarget,
            Exception aException)
    {
        handleException(aLog, aFeedbackTarget, null, aException);
    }

    public static void handleException(Logger aLog, IFeedbackContributor aFeedbackTarget,
            AjaxRequestTarget aTarget, Exception aException)
    {
        if (aException instanceof ReplaceHandlerException) {
            // Let Wicket redirects still work
            throw (ReplaceHandlerException) aException;
        }

        if (aTarget != null && aFeedbackTarget instanceof Component) {
            var component = (Component) aFeedbackTarget;
            aTarget.addChildren(component.getPage(), IFeedback.class);
        }

        try {
            throw aException;
        }
        catch (CommonException e) {
            aFeedbackTarget.error("Error: " + e.getMessage());
        }
        catch (UIMAException e) {
            aFeedbackTarget.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            aLog.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aFeedbackTarget.error("Error: " + e.getMessage());
            aLog.error("Error: " + e.getMessage(), e);
        }
    }
}
