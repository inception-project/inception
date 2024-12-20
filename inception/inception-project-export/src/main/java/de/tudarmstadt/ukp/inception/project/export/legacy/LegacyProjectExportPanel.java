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
package de.tudarmstadt.ukp.inception.project.export.legacy;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.time.LocalDateTime.now;
import static java.util.Objects.nonNull;

import java.time.format.DateTimeFormatter;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.settings.FormatDropdownChoice;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadBehavior;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 *
 * @deprecated Old export page code - to be removed in a future release.
 */
@Deprecated
public class LegacyProjectExportPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 2116717853865353733L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ProjectExportService exportService;
    private @SpringBean DocumentImportExportService importExportService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean UserDao userRepository;

    private Form<FullProjectExportRequest> form;
    private ProgressBar fileGenerationProgress;
    private LambdaAjaxLink cancelLink;
    private AjaxDownloadBehavior exportProjectDownload;

    private boolean exportInProgress = false;
    private ProjectExportTaskHandle exportTask;

    public LegacyProjectExportPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        add(form = new Form<>("exportForm", new CompoundPropertyModel<>(
                new FullProjectExportRequest(FullProjectExportRequest.FORMAT_AUTO, true))));

        form.add(createFormatDropdown("format"));
        form.add(createExportProjectLink("exportProject", form.getModel()));
        form.add(createExportCuratedDocumentsLink("exportCurated", form.getModel()));

        cancelLink = new LambdaAjaxLink("cancel", this::actionCancel);
        cancelLink.add(visibleWhen(() -> exportInProgress));
        form.add(cancelLink);

        exportProjectDownload = new AjaxDownloadBehavior(
                LoadableDetachableModel.of(() -> getFilename(exportTask)),
                LoadableDetachableModel.of(() -> getExport(exportTask)));
        form.add(exportProjectDownload);

        fileGenerationProgress = createProjectExportPreparationProgressBar("progress",
                new ExportProgressModel(LoadableDetachableModel
                        .of(() -> exportService.getTaskMonitor(exportTask))));
        form.add(fileGenerationProgress);

        form.add(
                new WebMarkupContainer("downloadWarning").add(visibleWhen(() -> exportInProgress)));
    }

    private ProgressBar createProjectExportPreparationProgressBar(String aId,
            ExportProgressModel aModel)
    {
        ProgressBar progressBar = new ProgressBar(aId, aModel)
        {
            private static final long serialVersionUID = -6599620911784164177L;

            @Override
            protected void onFinished(AjaxRequestTarget aTarget)
            {
                actionExportPreparationComplete(aTarget);
            }
        };
        progressBar.setVisible(false);
        return progressBar;
    }

    private AjaxLink<Void> createExportProjectLink(String aId,
            IModel<FullProjectExportRequest> aRequest)
    {
        AjaxLink<Void> link = new AjaxLink<Void>(aId)
        {
            private static final long serialVersionUID = -5758406309688341664L;

            @Override
            public void onClick(final AjaxRequestTarget target)
            {
                target.add(LegacyProjectExportPanel.this.getPage());
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                var request = aRequest.getObject();
                request.setProject(LegacyProjectExportPanel.this.getModelObject());
                exportInProgress = true;
                exportTask = exportService.startProjectExportTask(request,
                        authentication.getName());
                fileGenerationProgress.start(target);
            }
        };
        link.add(enabledWhen(() -> !exportInProgress));

        return link;
    }

    private DropDownChoice<String> createFormatDropdown(String aId)
    {
        DropDownChoice<String> format = new FormatDropdownChoice(aId);
        // Needed to update the model with the selection because the DownloadLink does
        // not trigger a form submit.
        format.add(new FormComponentUpdatingBehavior());
        return format;
    }

    private AjaxLink<Void> createExportCuratedDocumentsLink(String aId,
            IModel<FullProjectExportRequest> aRequest)
    {
        AjaxLink<Void> link = new AjaxLink<Void>(aId)
        {
            private static final long serialVersionUID = -5758406309688341664L;

            @Override
            public void onClick(final AjaxRequestTarget target)
            {
                target.add(LegacyProjectExportPanel.this.getPage());
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                var request = aRequest.getObject();
                request.setProject(LegacyProjectExportPanel.this.getModelObject());
                exportInProgress = true;
                exportTask = exportService.startProjectExportCuratedDocumentsTask(request,
                        authentication.getName());
                fileGenerationProgress.start(target);
            }
        };
        link.add(enabledWhen(() -> !exportInProgress));
        link.add(visibleWhen(() -> {
            Project project = LegacyProjectExportPanel.this.getModelObject();
            return nonNull(project) ? documentService.existsCurationDocument(project) : false;
        }));
        return link;
    }

    private String getFilename(ProjectExportTaskHandle aHandle)
    {
        var request = exportService.getExportRequest(aHandle);

        var formattedDateTime = now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        var filename = request.getFilenamePrefix() + "-" + request.getProject().getSlug() + "-"
                + formattedDateTime + ".zip";

        return filename;
    }

    private FileResourceStream getExport(ProjectExportTaskHandle aHandle)
    {
        ProjectExportTaskMonitor monitor = exportService.getTaskMonitor(aHandle);
        return new FileResourceStream(monitor.getExportedFile());
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        exportService.cancelTask(exportTask);
        aTarget.add(cancelLink);
    }

    private void actionExportPreparationComplete(AjaxRequestTarget aTarget)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(form);

        ProjectExportTaskMonitor monitor = exportService.getTaskMonitor(exportTask);

        monitor.getMessages().forEach(m -> m.toWicket(this));

        switch (monitor.getState()) {
        case COMPLETED:
            exportProjectDownload.initiate(aTarget);
            exportInProgress = false;
            info("Project export complete");
            break;
        case FAILED:
            exportInProgress = false;
            error("Project export failed");
            break;
        case CANCELLED:
            exportInProgress = false;
            info("Project export cancelled");
            break;
        default:
            error("Invalid project export state after export: " + monitor.getState());
        }

        fileGenerationProgress.setVisible(false);
    }
}
