/*
 * Copyright 2017
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
 */package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docinfo;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;

public class DocumentInfoSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 6127948490101336779L;

    public DocumentInfoSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        add(new Label("id", PropertyModel.of(aModel, "document.name")));
        add(new Label("format", PropertyModel.of(aModel, "document.format")));
        add(new Label("timestamp", PropertyModel.of(aModel, "document.timestamp")));
    }
}
