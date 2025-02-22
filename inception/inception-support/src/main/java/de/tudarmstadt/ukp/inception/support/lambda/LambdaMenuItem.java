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

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.LoggerFactory;
import org.wicketstuff.jquery.ui.widget.menu.MenuItem;

import de.tudarmstadt.ukp.inception.support.wicket.CommonException;

public class LambdaMenuItem
    extends MenuItem
{
    private static final long serialVersionUID = -2363349272322540838L;

    private AjaxCallback action;

    public LambdaMenuItem(String aTitle, AjaxCallback aCallback)
    {
        super(aTitle);

        action = aCallback;
    }

    public LambdaMenuItem(String aTitle, String aIconCss, AjaxCallback aCallback)
    {
        super(aTitle, aIconCss);

        action = aCallback;
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
        catch (CommonException e) {
            Page page = (Page) PageRequestHandlerTracker.getLastHandler(RequestCycle.get())
                    .getPage();
            page.error("Error: " + e.getMessage());
            aTarget.addChildren(page, IFeedback.class);
        }
        catch (Exception e) {
            Page page = (Page) PageRequestHandlerTracker.getLastHandler(RequestCycle.get())
                    .getPage();
            LoggerFactory.getLogger(page.getClass()).error("Error: " + e.getMessage(), e);
            page.error("Error: " + e.getMessage());
            aTarget.addChildren(page, IFeedback.class);
        }
    }
}
