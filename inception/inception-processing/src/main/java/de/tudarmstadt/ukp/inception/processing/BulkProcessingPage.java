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
package de.tudarmstadt.ukp.inception.processing;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;
import de.tudarmstadt.ukp.inception.ui.scheduling.TaskMonitorPanel;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/process")
public class BulkProcessingPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -8640092578172550838L;

    private static final PackageResourceReference NO_THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "no-thumbnail.svg");

    private static final String MID_DIALOG = "dialog";

    private @SpringBean UserDao userRepository;
    private @SpringBean BulkProcessorRegistry bulkProcessorRegistry;

    private final IModel<List<BulkProcessor>> processors;
    private final BootstrapModalDialog dialog;

    public BulkProcessingPage(PageParameters aParameters)
    {
        super(aParameters);

        var user = userRepository.getCurrentUser();

        requireProjectRole(user, MANAGER);

        dialog = new BootstrapModalDialog(MID_DIALOG);
        dialog.trapFocus();
        queue(dialog);

        processors = LoadableDetachableModel.of(this::listBulkProcessors);
        add(LambdaBehavior.onDetach(processors::detach));

        var processorList = new ListView<BulkProcessor>("processors", processors)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<BulkProcessor> aItem)
            {
                aItem.queue(new LambdaAjaxLink("create",
                        _target -> actionConfigureProcessor(_target, aItem.getModelObject())));
                aItem.queue(new Label("name", aItem.getModel().map(BulkProcessor::getName)));
                aItem.queue(new MarkdownLabel("description", aItem.getModel()
                        .map(BulkProcessor::getDescription).map($ -> $.orElse("No description"))));
                aItem.queue(new Image("thumbnail", aItem.getModel().map(BulkProcessor::getThumbnail)
                        .map($ -> $.orElse(NO_THUMBNAIL))));
            }
        };
        queue(processorList);

        var container = new WebMarkupContainer("processorsContainer");
        container.setOutputMarkupId(true);
        queue(container);

        queue(new TaskMonitorPanel("runningProcesses", getProject()) //
                .setPopupMode(false) //
                .setShowFinishedTasks(true));
    }

    protected void actionConfigureProcessor(AjaxRequestTarget aTarget, BulkProcessor aProcessor)
    {
        dialog.open(aProcessor.createPanel(ModalDialog.CONTENT_ID, getProjectModel()), aTarget);
    }

    private List<BulkProcessor> listBulkProcessors()
    {
        return bulkProcessorRegistry.getExtensions();
    }
}
