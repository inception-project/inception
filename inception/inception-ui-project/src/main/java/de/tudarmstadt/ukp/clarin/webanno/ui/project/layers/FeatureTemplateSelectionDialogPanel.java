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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.layers;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.FeatureInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;

public class FeatureTemplateSelectionDialogPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 2112018755924139726L;

    private static final PackageResourceReference NO_THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "no-thumbnail.svg");

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private LambdaAjaxLink closeDialogButton;

    public FeatureTemplateSelectionDialogPanel(String aId, IModel<Project> aProjectModel,
            IModel<List<FeatureInitializer>> aInitializers)
    {
        super(aId, aProjectModel);

        var initializers = new ListView<FeatureInitializer>("templates", aInitializers)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<FeatureInitializer> aItem)
            {
                aItem.queue(new LambdaAjaxLink("create",
                        _target -> actionCreateLayer(_target, aItem.getModelObject())));
                aItem.queue(new Label("name", aItem.getModel().map(FeatureInitializer::getName)));
                aItem.queue(new MarkdownLabel("description",
                        aItem.getModel().map(FeatureInitializer::getDescription)
                                .map($ -> $.orElse("No description"))));
                aItem.queue(new Image("thumbnail", aItem.getModel()
                        .map(FeatureInitializer::getThumbnail).map($ -> $.orElse(NO_THUMBNAIL))));
            }
        };
        queue(initializers);

        var container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        queue(container);

        closeDialogButton = new LambdaAjaxLink("closeDialog", this::actionCancel);
        closeDialogButton.setOutputMarkupId(true);
        queue(closeDialogButton);
    }

    private void actionCreateLayer(AjaxRequestTarget aTarget, FeatureInitializer aInitializer)
    {
        send(this, Broadcast.BUBBLE, new FeatureTemplateSelectedEvent(aTarget, aInitializer));
        findParent(ModalDialog.class).close(aTarget);
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }
}
