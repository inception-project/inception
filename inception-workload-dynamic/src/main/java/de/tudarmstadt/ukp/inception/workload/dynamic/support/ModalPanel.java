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

package de.tudarmstadt.ukp.inception.workload.dynamic.support;


import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class ModalPanel extends Panel
{

    private static final long serialVersionUID = 2797336810690526392L;

    public ModalPanel(String aID, SourceDocument aDocument)
    {
        super(aID);


        Label documentName = new Label("documentName", "Document name: "
            + aDocument.getName());
        Label size = new Label("size", "Document size: "
            + "");
        Label createdDate = new Label("createdDate", "Created Date: "
            +  aDocument.getCreated());
        //TODO List all users in Progress and Finished for the document
        Label source = new Label("source", "Source of the Document: "
            + aDocument.getId());


        Label userInProgress = new Label("userInProgress", "Users working on the Document: "
            + "");
        Label userFinished = new Label("userFinished", "Users finished the document: "
            + "");

        add(documentName);
        add(size);
        add(createdDate);
        add(source);
        add(userInProgress);
        add(userFinished);

    }
}
