/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@MountPath("/documentDetails.html")
public class DocumentDetailsPage extends ApplicationPageBase
{
    private final WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");

    public DocumentDetailsPage(String aId, String aText)
    {
        mainContainer.add(new Label("title", aId));
        Label textElement = new Label("text", aText);
        textElement.setOutputMarkupId(true);
        textElement.setEscapeModelStrings(false);
        mainContainer.add(textElement);
        add(mainContainer);
    }
}
