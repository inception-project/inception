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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.export;

import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.export.ExportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectPage;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 */
public class ProjectExportPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 2116717853865353733L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectPage.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    //private @SpringBean ExportService exportService;
    private @SpringBean ProjectExportService exportService;
    private @SpringBean ImportExportService importExportService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean UserDao userRepository;

    private ProgressBar fileGenerationProgress;
    @SuppressWarnings("unused")
    private AjaxLink<Void> exportProjectLink;

    private String fileName;
    private String downloadedFile;
    @SuppressWarnings("unused")
    private String projectName;

    private transient Thread thread = null;
    private transient FileGenerator runnable = null;

    private boolean enabled = true;
    private State exportState = State.NOT_STARTED;

    public ProjectExportPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        add(new ProjectExportForm("exportForm", aProjectModel));
    }

    public class ProjectExportForm
        extends Form<ProjectExportRequest>
    {
        private static final long serialVersionUID = 9151007311548196811L;

        public ProjectExportForm(String id, IModel<Project> aProject)
        {
            super(id, new CompoundPropertyModel<>(
                    new ProjectExportRequest(ProjectExportRequest.FORMAT_AUTO, true)));
            
            DropDownChoice<String> format = new DropDownChoice<>("format",
                    LoadableDetachableModel.of(() -> {
                        List<String> formats = importExportService.getWritableFormats().stream()
                                .map(FormatSupport::getName)
                                .sorted()
                                .collect(Collectors.toCollection(ArrayList::new));
                        formats.add(0, ProjectExportRequest.FORMAT_AUTO);
                        return formats;
                    }));
            // Needed to update the model with the selection because the DownloadLink does
            // not trigger a form submit.
            format.add(new FormComponentUpdatingBehavior());
            add(format);
            
            add(new DownloadLink("export", new LoadableDetachableModel<File>() {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load() {
                    File exportFile = null;
                    File exportTempDir = null;
                    try {
                        exportTempDir = File.createTempFile("webanno", "export");
                        exportTempDir.delete();
                        exportTempDir.mkdirs();

                        boolean curationDocumentExist = documentService.existsCurationDocument(
                                ProjectExportForm.this.getModelObject().getProject());

                        if (!curationDocumentExist) {
                            error("No curation document created yet for this document");
                        } else {
                            ExportUtil.exportCuratedDocuments(documentService, importExportService,
                                    ProjectExportForm.this.getModelObject(), exportTempDir, false);
                            ZipUtils.zipFolder(exportTempDir, new File(
                                    exportTempDir.getAbsolutePath() + ".zip"));
                            exportFile = new File(exportTempDir.getAbsolutePath()
                                    + ".zip");

                        }
                    }
                    catch (CASRuntimeException e) {
                        cancelOperationOnError();
                        error("Error: " + e.getMessage());
                    }
                    catch (Exception e) {
                        error("Error: " + e.getMessage());
                        cancelOperationOnError();
                    }
                    finally {
                        try {
                            FileUtils.forceDelete(exportTempDir);
                        } catch (IOException e) {
                            error("Unable to delete temp file");
                        }
                    }

                    return exportFile;
                }

                private void cancelOperationOnError()
                {
                    if (thread != null) {
                        ProjectExportForm.this.getModelObject().progress = 100;
                        thread.interrupt();
                    }
                }
            }, new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 2591915908792854707L;
//                Provide meaningful name to curated documents zip
                @Override
                protected String load()
                {
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmm");
                    return ProjectExportForm.this.getModelObject().getProject().getName() +
                        "_curated_documents_" + fmt.format(new Date()) + ".zip";
                }
            }) {
                private static final long serialVersionUID = 5630612543039605914L;

                @Override
                public boolean isVisible() {
                    Project project = ProjectExportPanel.this.getModelObject();
                    return nonNull(project) ? documentService.existsCurationDocument(project)
                            : false;
                }

                @Override
                public boolean isEnabled() {
                    return enabled;

                }
                
                @Override
                public void onClick()
                {
                    try {
                        super.onClick();
                    }
                    catch (IllegalStateException e) {
                        LOG.error("Error: {}", e.getMessage(), e);
                        error("Unable to export curated documents because of exception while processing.");
                    }
                }
            }.setDeleteAfterDownload(true)).setOutputMarkupId(true);

            final AJAXDownload exportProject = new AJAXDownload() {
                private static final long serialVersionUID = 2005074740832698081L;

                @Override
                protected String getFileName() {
                    String name;
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmm");
                    try {
                        name = URLEncoder.encode(
                                ProjectExportForm.this.getModelObject().getProject().getName(),
                                "UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        name = super.getFileName();
                    }
                    
                    name = FilenameUtils.removeExtension(name);
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
                    return new Progression(ProjectExportForm.this.getModelObject().progress);
                }
            })
            {
                private static final long serialVersionUID = -6599620911784164177L;

                @Override
                protected void onFinished(AjaxRequestTarget target)
                {
                    target.addChildren(getPage(), IFeedback.class);

                    while (!runnable.getMessages().isEmpty()) {
                        LogMessage msg = runnable.getMessages().poll();
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
                    
                    switch (exportState) {
                    case COMPLETED:
                        if (!fileName.equals(downloadedFile)) {
                            exportProject.initiate(target, fileName);
                            downloadedFile = fileName;
                            
                            enabled = true;
                            info("Project export complete");
                        }
                        break;
                    case FAILED:
                        enabled = true;
                        error("Project export failed");
                        break;
                    case CANCELLED:
                        enabled = true;
                        info("Project export cancelled");
                        break;
                    default:
                        error("Invalid project export state after export: " + exportProject);
                    }
                }
            };

            fileGenerationProgress.add(exportProject);
            add(fileGenerationProgress);

            add(exportProjectLink = new AjaxLink<Void>("exportProject") {
                private static final long serialVersionUID = -5758406309688341664L;

                @Override
                public boolean isEnabled() {
                    return enabled;
                }

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    enabled = false;
                    exportState = State.FAILED;
                    ProjectExportForm.this.getModelObject().progress = 0;
                    target.add(ProjectExportPanel.this.getPage());
                    fileGenerationProgress.start(target);
                    Authentication authentication = SecurityContextHolder.getContext()
                            .getAuthentication();
                    ProjectExportRequest request = ProjectExportForm.this.getModelObject();
                    request.setProject(ProjectExportPanel.this.getModelObject());
                    runnable = new FileGenerator(request, authentication.getName());
                    thread = new Thread(runnable);
                    thread.start();
                }
            });

            add(new AjaxLink<Void>("cancel") {
                private static final long serialVersionUID = 5856284172060991446L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    if (thread != null) {
                        ProjectExportForm.this.getModelObject().progress = 100;
                        exportState = State.CANCELLED;
                        thread.interrupt();
                    }
                }

                @Override
                public boolean isEnabled()
                {
                    // Enabled only if the export button has been disabled (during export)
                    return (!enabled) ;
                }
            });
        }
    }
    
    enum State {
        NOT_STARTED, COMPLETED, CANCELLED, FAILED;
    }
    
    public class FileGenerator
        implements Runnable
    {
        private String username;
        private ProjectExportRequest model;

        public FileGenerator(ProjectExportRequest aModel, String aUsername)
        {
            model = aModel;
            username = aUsername;
        }

        @Override
        public void run()
        {
            // We are in a new thread. Set up thread-specific MDC
            MDC.put(Logging.KEY_USERNAME, username);
            MDC.put(Logging.KEY_PROJECT_ID, String.valueOf(model.getProject().getId()));
            MDC.put(Logging.KEY_REPOSITORY_PATH, documentService.getDir().toString());
            
            File file;
            try {
                Thread.sleep(100); // Why do we sleep here?
                file = exportService.exportProject(model);
                fileName = file.getAbsolutePath();
                projectName = model.getProject().getName();
                exportState = State.COMPLETED;
            }
            catch (Throwable e) {
                LOG.error("Unexpected error during project export", e);
                model.addMessage(LogMessage.error(this, "Unexpected error during project export: %s",
                                ExceptionUtils.getRootCauseMessage(e)));
                if (thread != null) {
                    exportState = State.FAILED;
                    // This marks the progression as complete and causes ProgressBar#onFinished
                    // to be called where we display the messages
                    model.progress = 100; 
                    thread.interrupt();
                }
            }
        }

        public Queue<LogMessage> getMessages()
        {
            return model.getMessages();
        }
    }
}
