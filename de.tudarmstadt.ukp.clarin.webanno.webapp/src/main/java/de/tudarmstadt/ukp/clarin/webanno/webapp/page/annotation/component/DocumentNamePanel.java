/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.export.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.export.model.SourceDocument;

/**
 * A {@link Panel} which contains a {@link Label} to display document name as concatenations of
 * {@link Project#getName()} and {@link SourceDocument#getName()}
 *
 * @author Seid Muhie Yimam
 */
public class DocumentNamePanel
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    ModalWindow yesNoModal;

    public DocumentNamePanel(String id, final IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);
        add(new Label("doumentName", new LoadableDetachableModel<String>()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                String projectName;
                String documentName;
                if (aModel.getObject().getProject() == null) {
                    projectName = "/";
                }
                else {
                    projectName = aModel.getObject().getProject().getName() + "/";
                }
                if (aModel.getObject().getDocument() == null) {
                    documentName = "";
                }
                else {
                    documentName = aModel.getObject().getDocument().getName();
                }
                return projectName + documentName;

            }
        }).setOutputMarkupId(true));
    }

}
