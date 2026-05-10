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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.PubAnnotationProvider;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class PubAnnotationProviderTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean ExternalSearchProviderFactory<PubAnnotationProviderTraits> externalSearchProviderFactory;
    private final DocumentRepository documentRepository;
    private final PubAnnotationProviderTraits properties;

    private final BootstrapModalDialog dialog;
    private final TextField<String> projectField;

    public PubAnnotationProviderTraitsEditor(String aId,
            IModel<DocumentRepository> aDocumentRepository)
    {
        super(aId, aDocumentRepository);
        documentRepository = aDocumentRepository.getObject();
        properties = externalSearchProviderFactory.readTraits(documentRepository);

        dialog = new BootstrapModalDialog("dialog");
        dialog.trapFocus();
        queue(dialog);

        var form = new Form<PubAnnotationProviderTraits>(MID_FORM,
                CompoundPropertyModel.of(Model.of(properties)))
        {
            private static final long serialVersionUID = -3109239608742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                externalSearchProviderFactory.writeTraits(documentRepository, properties);
            }
        };

        var remoteUrl = new TextField<String>("url");
        remoteUrl.setRequired(true);
        remoteUrl.add(new UrlValidator());
        form.add(remoteUrl);

        projectField = new TextField<>("project");
        projectField.setOutputMarkupId(true);
        form.add(projectField);

        form.add(new LambdaAjaxLink("browseProjects", this::actionBrowseProjects));

        add(form);
    }

    private void actionBrowseProjects(AjaxRequestTarget aTarget)
    {
        try {
            var provider = (PubAnnotationProvider) ((ExternalSearchProviderFactory<?>) externalSearchProviderFactory)
                    .getNewExternalSearchProvider();
            var projects = provider.listProjects(properties);
            var content = new PubAnnotationProjectSelectionDialogPanel(
                    BootstrapModalDialog.CONTENT_ID, projects, this::actionSelectProject);
            dialog.open(content, aTarget);
        }
        catch (Exception e) {
            error("Could not load projects from " + properties.getUrl() + ": " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void actionSelectProject(AjaxRequestTarget aTarget,
            de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationProject aProject)
    {
        properties.setProject(aProject.getName());
        aTarget.add(projectField);
    }
}
