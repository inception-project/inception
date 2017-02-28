/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.export.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.export.SourceDocument;

/**
 * A {@link Panel} which contains a {@link Label} to display document name as concatenations of
 * {@link Project#getName()} and {@link SourceDocument#getName()}
 */
public class DocumentNamePanel
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    public DocumentNamePanel(String id, final IModel<AnnotatorState> aModel)
    {
        super(id, aModel);
        add(new Label("doumentName", new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                StringBuilder sb = new StringBuilder();
                
                if (aModel.getObject().getProject() != null) {
                    sb.append(aModel.getObject().getProject().getName());
                }

                sb.append("/");

                if (aModel.getObject().getDocument() != null) {
                    sb.append(aModel.getObject().getDocument().getName());
                }
                
                if (RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType())) {
                    sb.append(" (");
                    if (aModel.getObject().getProject() != null) {
                        sb.append(aModel.getObject().getProject().getId());
                    }
                    sb.append("/");
                    if (aModel.getObject().getDocument() != null) {
                        sb.append(aModel.getObject().getDocument().getId());
                    }
                    sb.append(")");
                }
                
                return sb.toString();

            }
        }).setOutputMarkupId(true));
    }

}
