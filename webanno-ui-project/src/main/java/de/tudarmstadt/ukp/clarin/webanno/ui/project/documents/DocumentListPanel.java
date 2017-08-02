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
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

public class DocumentListPanel
    extends Panel
{
    private static final long serialVersionUID = -4981251367691500470L;

    private static final Logger LOG = LoggerFactory.getLogger(DocumentListPanel.class);

    private @SpringBean DocumentService documentService;

    private OverviewListChoice<SourceDocument> overviewList;
    private IModel<Project> project;
    private IModel<SourceDocument> document;
    
    public DocumentListPanel(String aId, IModel<Project> aProject)
    {
        super(aId);
     
        setOutputMarkupId(true);
        
        project = aProject;
        document = Model.of();

        Form<Void> form = new Form<>("form");
        add(form);
        
        overviewList = new OverviewListChoice<>("documents");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(document);
        overviewList.setChoices(LambdaModel.of(this::listSourceDocuments));
        form.add(overviewList);

        ConfirmationDialog confirmationDialog = new ConfirmationDialog("confirmationDialog");
        confirmationDialog.setConfirmAction(this::actionDelete);
        add(confirmationDialog);

        LambdaAjaxButton<Void> delete = new LambdaAjaxButton<>("delete", (t, f) -> 
                confirmationDialog.show(t));
        form.add(delete);
    }
    
    private List<SourceDocument> listSourceDocuments()
    {
        return documentService.listSourceDocuments(project.getObject());
    }
    
    private void actionDelete(AjaxRequestTarget aTarget)
    {
        if (document.getObject() == null) {
            error("No document selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        try {
            documentService.removeSourceDocument(document.getObject());
        }
        catch (IOException e) {
            LOG.error("Unable to delete document", e);
            error("Unable to delete document: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        document.setObject(null);
        aTarget.add(getPage());
    }
}
