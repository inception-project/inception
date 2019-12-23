/*
 * Copyright 2019
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

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.ui.widget.menu.MenuItem;

public class LambdaMenuItem extends MenuItem
{
    private static final long serialVersionUID = -2363349272322540838L;
    
    private AjaxCallback action;
    
    public LambdaMenuItem(String aTitle, AjaxCallback aCallback)
    {
        super(aTitle);
        
        action = aCallback;
    }

    @Override
    public void onClick(AjaxRequestTarget aTarget)
    {
        try {
            action.accept(aTarget);
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
