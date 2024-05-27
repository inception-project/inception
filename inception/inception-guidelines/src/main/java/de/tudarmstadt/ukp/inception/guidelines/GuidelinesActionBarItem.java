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
package de.tudarmstadt.ukp.inception.guidelines;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class GuidelinesActionBarItem
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private GuidelinesDialog guidelinesDialog;

    private @SpringBean GuidelinesService guidelinesService;

    public GuidelinesActionBarItem(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        add(guidelinesDialog = new GuidelinesDialog("guidelinesDialog", aPage.getModel()));
        add(new LambdaAjaxLink("showGuidelinesDialog", guidelinesDialog::show));

        // Hide the guidelines button if there are no guidelines
        add(visibleWhen(
                () -> guidelinesService.hasGuidelines(aPage.getModelObject().getProject())));
    }
}
