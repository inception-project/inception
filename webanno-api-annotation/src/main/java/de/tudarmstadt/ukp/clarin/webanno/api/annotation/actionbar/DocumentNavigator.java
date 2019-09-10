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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Page_up;
import static wicket.contrib.input.events.key.KeyType.Shift;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class DocumentNavigator
    extends Panel
{
    private static final long serialVersionUID = 7061696472939390003L;

    private AnnotationPageBase annotationPage;

    public DocumentNavigator(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        annotationPage = aPage;

        add(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_up }, click)));

        add(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_down }, click)));
    }

    /**
     * Show the previous document, if exist
     */
    protected void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        annotationPage.getModelObject().moveToPreviousDocument(annotationPage.getListOfDocs());
        annotationPage.actionLoadDocument(aTarget);
    }

    /**
     * Show the next document if exist
     */
    protected void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        annotationPage.getModelObject().moveToNextDocument(annotationPage.getListOfDocs());
        annotationPage.actionLoadDocument(aTarget);
    }
}
