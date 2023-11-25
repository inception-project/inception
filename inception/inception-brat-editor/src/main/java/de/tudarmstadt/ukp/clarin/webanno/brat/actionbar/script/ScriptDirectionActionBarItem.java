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
package de.tudarmstadt.ukp.clarin.webanno.brat.actionbar.script;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class ScriptDirectionActionBarItem
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private final AnnotationPageBase page;

    public ScriptDirectionActionBarItem(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        add(new LambdaAjaxLink("toggleScriptDirection", this::actionToggleScriptDirection));
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget) throws Exception
    {
        page.getModelObject().toggleScriptDirection();
        page.actionRefreshDocument(aTarget);
    }
}
