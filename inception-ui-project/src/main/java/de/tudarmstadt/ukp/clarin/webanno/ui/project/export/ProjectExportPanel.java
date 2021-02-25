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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectSettingsPage;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 */
public class ProjectExportPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 2116717853865353733L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectSettingsPage.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ProjectExportService exportService;
    private @SpringBean ImportExportService importExportService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean UserDao userRepository;

    private ProgressBar fileGenerationProgress;
    private AjaxLink<Void> exportProjectLink;
    private AjaxLink<Void> exportCurateLink;

    private boolean exportInProgress = false;
    private ProjectExportTaskHandle exportTask;

    public ProjectExportPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        add(new ProjectExportForm("exportForm", aProjectModel));
    }

    public class ProjectExportForm
        extends Form<ProjectExportRequest>
    {
        private static final long serialVersionUID = 9151007311548196811L;

        private LambdaAjaxLink cancelLink;

        public ProjectExportForm(String id, IModel<Project> aProject)
        {
            super(id, new CompoundPropertyModel<>(
                    new ProjectExportRequest(ProjectExportRequest.FORMAT_AUTO, true)));

            DropDownChoice<String> format = new BootstrapSelect<>("format");
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
            add(format);

            final AJAXDownload exportProject = new AJAXDownload()
            {
                private static final long serialVersionUID = 2005074740832698081L;

                @Override
                protected String getFileName()
                {
                    ProjectExportRequest request = exportService.getExportRequest(exportTask);
                    String name;
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmm");
                    try {
                        name = URLEncoder.encode(request.getProject().getName(), "UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        name = super.getFileName();
                    }

                    name = FilenameUtils.removeExtension(name);
                    if (isNotBlank(request.getFilenameTag())) {
                        name += request.getFilenameTag();
                    }
                    name += "_" + fmt.format(new Date()) + ".zip";

                    return name;
                }
            };

            fileGenerationProgress = new ProgressBar("progress", new ProgressionModel()
            {
                private static final long serialVersionUID = 1971929040248482474L;

                @Override
                protected Progression getProgression()
                {
                    ProjectExportTaskMonitor monitor = exportService.getTaskMonitor(exportTask);
                    if (monitor != null) {
                        Optional<LogMessage> msg = Optional
                                .ofNullable(monitor.getMessages().peek());
                        return new Progression(monitor.getProgress(),
                                msg.map(LogMessage::getMessage).orElse(null));
                    }
                    else {
                        return new Progression(0, "Export not started yet...");
                    }
                }
            })
            {
                private static final long serialVersionUID = -6599620911784164177L;

                @Override
                protected void onFinished(AjaxRequestTarget target)
                {
                    target.addChildren(getPage(), IFeedback.class);
                    target.add(ProjectExportForm.this);

                    ProjectExportTaskMonitor monitor = exportService.getTaskMonitor(exportTask);

                    while (!monitor.getMessages().isEmpty()) {
                        LogMessage msg = monitor.getMessages().poll();
                        switch (msg.getLevel()) {
                        case INFO:
                            info(msg.getMessage());
                            break;
                        case WARN:
                            warn(msg.getMessage());
                            break;
                        case ERROR:
                            error(msg.getMessage());
                            break;
                        default:
                            error(msg.getMessage());
                            break;
                        }
                    }

                    switch (monitor.getState()) {
                    case COMPLETED:
                        exportProject.initiate(target, monitor.getExportedFile().getAbsolutePath());
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
                }
            };

            fileGenerationProgress.add(exportProject);
            add(fileGenerationProgress);

            add(exportProjectLink = new AjaxLink<Void>("exportProject")
            {
                private static final long serialVersionUID = -5758406309688341664L;

                @Override
                public void onClick(final AjaxRequestTarget target)
                {
                    target.add(ProjectExportPanel.this.getPage());
                    Authentication authentication = SecurityContextHolder.getContext()
                            .getAuthentication();
                    ProjectExportRequest request = ProjectExportForm.this.getModelObject();
                    request.setProject(ProjectExportPanel.this.getModelObject());
                    request.setFilenameTag("_project");
                    exportInProgress = true;
                    exportTask = exportService.startProjectExportTask(request,
                            authentication.getName());
                    fileGenerationProgress.start(target);
                }
            });
            exportProjectLink.add(enabledWhen(() -> !exportInProgress));

            add(exportCurateLink = new AjaxLink<Void>("exportCurated")
            {
                private static final long serialVersionUID = -5758406309688341664L;

                @Override
                public void onClick(final AjaxRequestTarget target)
                {
                    target.add(ProjectExportPanel.this.getPage());
                    Authentication authentication = SecurityContextHolder.getContext()
                            .getAuthentication();
                    ProjectExportRequest request = ProjectExportForm.this.getModelObject();
                    request.setProject(ProjectExportPanel.this.getModelObject());
                    request.setFilenameTag("_curated_documents");
                    exportInProgress = true;
                    exportTask = exportService.startProjectExportCuratedDocumentsTask(request,
                            authentication.getName());
                    fileGenerationProgress.start(target);
                }
            });
            exportCurateLink.add(enabledWhen(() -> !exportInProgress));
            exportCurateLink.add(visibleWhen(() -> {
                Project project = ProjectExportPanel.this.getModelObject();
                return nonNull(project) ? documentService.existsCurationDocument(project) : false;
            }));

            cancelLink = new LambdaAjaxLink("cancel", this::actionCancel);
            cancelLink.add(enabledWhen(() -> exportInProgress));
            add(cancelLink);
        }

        private void actionCancel(AjaxRequestTarget aTarget)
        {
            exportService.cancelTask(exportTask);
            aTarget.add(cancelLink);
        }
    }
}
