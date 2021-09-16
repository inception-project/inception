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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.export;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 */
public class ProjectExportPanel
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

    private Form<ProjectExportRequest> form;
    private ProgressBar fileGenerationProgress;
    private LambdaAjaxLink cancelLink;
    private AjaxDownloadBehavior exportProjectDownload;

    private boolean exportInProgress = false;
    private ProjectExportTaskHandle exportTask;

    public ProjectExportPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        add(form = new Form<>("exportForm", new CompoundPropertyModel<>(
                new ProjectExportRequest(ProjectExportRequest.FORMAT_AUTO, true))));

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
            IModel<ProjectExportRequest> aRequest)
    {
        AjaxLink<Void> link = new AjaxLink<Void>(aId)
        {
            private static final long serialVersionUID = -5758406309688341664L;

            @Override
            public void onClick(final AjaxRequestTarget target)
            {
                target.add(ProjectExportPanel.this.getPage());
                Authentication authentication = SecurityContextHolder.getContext()
                        .getAuthentication();
                ProjectExportRequest request = aRequest.getObject();
                request.setProject(ProjectExportPanel.this.getModelObject());
                request.setFilenameTag("_project");
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
        DropDownChoice<String> format = new DropDownChoice<>(aId);
        format.setChoiceRenderer(new ChoiceRenderer<String>()
        {
            private static final long serialVersionUID = -6139450455463062998L;

            @Override
            public Object getDisplayValue(String aObject)
            {
                if (ProjectExportRequest.FORMAT_AUTO.equals(aObject)) {
                    return ProjectExportRequest.FORMAT_AUTO;
                }

                return importExportService.getFormatById(aObject).get().getName();
            }
        });
        format.setChoices(LoadableDetachableModel.of(() -> {
            List<String> formats = importExportService.getWritableFormats().stream() //
                    .sorted(Comparator.comparing(FormatSupport::getName)) //
                    .map(FormatSupport::getId) //
                    .collect(toCollection(ArrayList::new));
            formats.add(0, ProjectExportRequest.FORMAT_AUTO);
            return formats;
        }));
        // Needed to update the model with the selection because the DownloadLink does
        // not trigger a form submit.
        format.add(new FormComponentUpdatingBehavior());
        return format;
    }

    private AjaxLink<Void> createExportCuratedDocumentsLink(String aId,
            IModel<ProjectExportRequest> aRequest)
    {
        AjaxLink<Void> link = new AjaxLink<Void>(aId)
        {
            private static final long serialVersionUID = -5758406309688341664L;

            @Override
            public void onClick(final AjaxRequestTarget target)
            {
                target.add(ProjectExportPanel.this.getPage());
                Authentication authentication = SecurityContextHolder.getContext()
                        .getAuthentication();
                ProjectExportRequest request = aRequest.getObject();
                request.setProject(ProjectExportPanel.this.getModelObject());
                request.setFilenameTag("_curated_documents");
                exportInProgress = true;
                exportTask = exportService.startProjectExportCuratedDocumentsTask(request,
                        authentication.getName());
                fileGenerationProgress.start(target);
            }
        };
        link.add(enabledWhen(() -> !exportInProgress));
        link.add(visibleWhen(() -> {
            Project project = ProjectExportPanel.this.getModelObject();
            return nonNull(project) ? documentService.existsCurationDocument(project) : false;
        }));
        return link;
    }

    private String getFilename(ProjectExportTaskHandle aHandle)
    {
        ProjectExportRequest request = exportService.getExportRequest(aHandle);

        String name = request.getProject().getSlug();

        if (isNotBlank(request.getFilenameTag())) {
            name += request.getFilenameTag();
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmm");
        name += "_" + fmt.format(new Date()) + ".zip";

        return name;
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
