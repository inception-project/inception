/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class ModalPanel extends Panel
{
    public ModalPanel(String aID, SourceDocument aDocument)
    {
        super(aID);


        Label documentName = new Label("documentName", "Document name: "
            + aDocument.getName());
        Label size = new Label("size", "Document size: "
            + aDocument.getId());
        Label createdDate = new Label("createdDate", "Created Date: "
            +  aDocument.getProject().getName());
        Label source = new Label("source", "Source of the Document: "
            + aDocument.getState().getName());

        add(documentName);
        add(size);
        add(createdDate);
        add(source);

    }
}
