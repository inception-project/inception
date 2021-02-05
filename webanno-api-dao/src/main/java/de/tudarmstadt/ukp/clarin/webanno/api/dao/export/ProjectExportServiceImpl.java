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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.NOT_STARTED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.RUNNING;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ConcurrentReferenceHashMap;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

@Component
public class ProjectExportServiceImpl
    implements ProjectExportService, DisposableBean
{
    public static final String EXPORTED_PROJECT = "exportedproject";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<ProjectExportTaskHandle, TaskInfo> tasks = new ConcurrentReferenceHashMap<>();
    private final ProjectService projectService;
    private final ExecutorService taskExecutorService;
    private final ScheduledExecutorService cleaningScheduler;
    private final ApplicationContext applicationContext;

    private final List<ProjectExporter> exportersProxy;
    private List<ProjectExporter> exporters;

    @Autowired
    public ProjectExportServiceImpl(ApplicationContext aApplicationContext,
            @Lazy @Autowired(required = false) List<ProjectExporter> aExporters,
            @Autowired ProjectService aProjectService)
    {
        applicationContext = aApplicationContext;
        exportersProxy = aExporters;
        projectService = aProjectService;

        taskExecutorService = Executors.newFixedThreadPool(4);

        cleaningScheduler = Executors.newScheduledThreadPool(1);
        cleaningScheduler.scheduleAtFixedRate(this::cleanUp, 15, 15, TimeUnit.MINUTES);
    }

    @Override
    public void destroy() throws Exception
    {
        taskExecutorService.shutdownNow();
        cleaningScheduler.shutdownNow();
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<ProjectExporter> exps = new ArrayList<>();

        if (exportersProxy != null) {
            exps.addAll(exportersProxy);
            AnnotationAwareOrderComparator.sort(exps);

            Set<Class<? extends ProjectExporter>> exporterClasses = new HashSet<>();
            for (ProjectExporter init : exps) {
                if (exporterClasses.add(init.getClass())) {
                    log.info("Found project exporter: {}",
                            ClassUtils.getAbbreviatedName(init.getClass(), 20));
                }
                else {
                    throw new IllegalStateException("There cannot be more than once instance "
                            + "of each project exporter class! Duplicate instance of class: "
                            + init.getClass());
                }
            }
        }

        exporters = Collections.unmodifiableList(exps);
    }

    @Override
    @Transactional
    public File exportProject(ProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException, IOException
    {
        boolean success = false;
        File exportTempDir = null;
        try {
            // Directory to store source documents and annotation documents
            exportTempDir = File.createTempFile("webanno-project", "export");
            exportTempDir.delete();
            exportTempDir.mkdirs();

            // Target file
            File projectZipFile = new File(exportTempDir.getAbsolutePath() + ".zip");

            ExportedProject exProjekt = exportProject(aRequest, aMonitor, exportTempDir);

            // all metadata and project settings data from the database as JSON file
            File projectSettings = File.createTempFile(EXPORTED_PROJECT, ".json");
            JSONUtil.generatePrettyJson(exProjekt, projectSettings);
            FileUtils.copyFileToDirectory(projectSettings, exportTempDir);

            try {
                ZipUtils.zipFolder(exportTempDir, projectZipFile);
            }
            finally {
                FileUtils.forceDelete(projectSettings);
                System.gc();
                FileUtils.forceDelete(exportTempDir);
            }

            success = true;

            return projectZipFile;
        }
        finally {
            if (!success && exportTempDir != null) {
                try {
                    FileUtils.forceDelete(exportTempDir);
                }
                catch (IOException e) {
                    aMonitor.addMessage(LogMessage.error(this,
                            "Unable to delete temporary export directory [%s]", exportTempDir));
                    log.error("Unable to delete temporary export directory [{}]", exportTempDir);
                }
            }
        }
    }

    private ExportedProject exportProject(ProjectExportRequest aRequest,
            ProjectExportTaskMonitor aMonitor, File aStage)
        throws ProjectExportException, IOException
    {
        Deque<ProjectExporter> deque = new LinkedList<>(exporters);
        Set<Class<? extends ProjectExporter>> initsSeen = new HashSet<>();
        Set<ProjectExporter> initsDeferred = SetUtils.newIdentityHashSet();

        ExportedProject exProject = new ExportedProject();
        exProject.setName(aRequest.getProject().getName());

        try {
            while (!deque.isEmpty()) {
                ProjectExporter initializer = deque.pop();

                if (initsDeferred.contains(initializer)) {
                    throw new IllegalStateException("Circular initializer dependencies in "
                            + initsDeferred + " via " + initializer);
                }

                if (initsSeen.containsAll(initializer.getExportDependencies())) {
                    log.debug("Applying project exporter: {}", initializer);
                    initializer.exportData(aRequest, aMonitor, exProject, aStage);
                    initsSeen.add(initializer.getClass());
                    initsDeferred.clear();
                }
                else {
                    log.debug(
                            "Deferring project exporter as dependencies are not yet fulfilled: [{}]",
                            initializer);
                    deque.add(initializer);
                    initsDeferred.add(initializer);
                }
            }
        }
        catch (IOException e) {
            // IOExceptions like java.nio.channels.ClosedByInterruptException should be thrown up
            // as-is. This allows us to handle export cancellation in the project export UI panel
            throw e;
        }
        catch (Exception e) {
            throw new ProjectExportException("Project export failed", e);
        }

        return exProject;
    }

    @Override
    @Transactional
    public Project importProject(ProjectImportRequest aRequest, ZipFile aZip)
        throws ProjectExportException
    {
        long start = currentTimeMillis();

        Deque<ProjectExporter> deque = new LinkedList<>(exporters);
        Set<Class<? extends ProjectExporter>> initsSeen = new HashSet<>();
        Set<ProjectExporter> initsDeferred = SetUtils.newIdentityHashSet();

        Project project = new Project();

        try {
            ExportedProject exProject = loadExportedProject(aZip);

            // If the name of the project is already taken, generate a new name
            String projectName = exProject.getName();
            if (projectService.existsProject(projectName)) {
                projectName = copyProjectName(projectName);
            }
            project.setName(projectName);

            // We need to set the mode here already because the mode is a non-null column.
            // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
            // during export but as lower-case in the database. This is compensating for this case.
            project.setMode(StringUtils.lowerCase(exProject.getMode(), Locale.US));

            // Initial saving of the project
            projectService.createProject(project);

            // Apply the importers
            while (!deque.isEmpty()) {
                ProjectExporter importer = deque.pop();

                if (initsDeferred.contains(importer)) {
                    throw new IllegalStateException("Circular initializer dependencies in "
                            + initsDeferred + " via " + importer);
                }

                if (initsSeen.containsAll(importer.getImportDependencies())) {
                    log.debug("Applying project importer: {}", importer);
                    importer.importData(aRequest, project, exProject, aZip);
                    initsSeen.add(importer.getClass());
                    initsDeferred.clear();
                }
                else {
                    log.debug(
                            "Deferring project exporter as dependencies are not yet fulfilled: [{}]",
                            importer);
                    deque.add(importer);
                    initsDeferred.add(importer);
                }
            }
        }
        catch (Exception e) {
            throw new ProjectExportException("Project import failed", e);
        }

        log.info("Imported project [{}]({}) ({})", project.getName(), project.getId(),
                formatDurationWords(currentTimeMillis() - start, true, true));

        return project;
    }

    /**
     * Get a project name to be used when importing. Use the prefix, copy_of_...+ i to avoid
     * conflicts
     */
    private String copyProjectName(String aProjectName)
    {
        String projectName = "copy_of_" + aProjectName;
        int i = 1;
        while (true) {
            if (projectService.existsProject(projectName)) {
                projectName = "copy_of_" + aProjectName + "(" + i + ")";
                i++;
            }
            else {
                return projectName;
            }
        }
    }

    public static ExportedProject loadExportedProject(ZipFile aZip) throws IOException
    {
        // Locate the project model in the ZIP file
        ZipEntry projectSettingsEntry = null;
        for (Enumeration<? extends ZipEntry> zipEnumerate = aZip.entries(); zipEnumerate
                .hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                projectSettingsEntry = entry;
                break;
            }
        }

        // Load the project model from the JSON file
        String text;
        try (InputStream is = aZip.getInputStream(projectSettingsEntry)) {
            text = IOUtils.toString(is, "UTF-8");
        }
        ExportedProject exProject = JSONUtil.getObjectMapper().readValue(text,
                ExportedProject.class);

        return exProject;
    }

    @Override
    public ProjectExportTaskHandle startProjectExportTask(ProjectExportRequest aRequest,
            String aUsername)
    {
        ProjectExportTaskHandle handle = new ProjectExportTaskHandle();
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor();
        ProjectExportFullProjectTask task = new ProjectExportFullProjectTask(handle, monitor,
                aRequest, aUsername);

        return startTask(task);
    }

    @Override
    public ProjectExportTaskHandle startProjectExportCuratedDocumentsTask(
            ProjectExportRequest aRequest, String aUsername)
    {
        ProjectExportTaskHandle handle = new ProjectExportTaskHandle();
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor();
        ProjectExportCuratedDocumentsTask task = new ProjectExportCuratedDocumentsTask(handle,
                monitor, aRequest, aUsername);

        return startTask(task);
    }

    private ProjectExportTaskHandle startTask(ProjectExportTask_ImplBase aTask)
    {
        ProjectExportTaskHandle handle = aTask.getHandle();

        // This autowires the task fields manually.
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(aTask);
        factory.initializeBean(aTask, "transientTask");

        tasks.put(handle, new TaskInfo(taskExecutorService.submit(aTask), aTask));

        return handle;
    }

    @Override
    public ProjectExportRequest getExportRequest(ProjectExportTaskHandle aHandle)
    {
        TaskInfo task = tasks.get(aHandle);

        if (task == null) {
            return null;
        }

        return task.task.getRequest();
    }

    @Override
    public ProjectExportTaskMonitor getTaskMonitor(ProjectExportTaskHandle aHandle)
    {
        TaskInfo task = tasks.get(aHandle);

        if (task == null) {
            return null;
        }

        return task.task.getMonitor();
    }

    @Override
    public boolean cancelTask(ProjectExportTaskHandle aHandle)
    {
        TaskInfo task = tasks.get(aHandle);

        if (task == null) {
            return false;
        }

        task.future.cancel(true);

        return true;
    }

    private void cleanUp()
    {
        for (Entry<ProjectExportTaskHandle, TaskInfo> e : tasks.entrySet()) {
            ProjectExportTaskMonitor monitor = e.getValue().task.getMonitor();

            // Do not clean up running tasks or tasks that have not started yet
            if (asList(NOT_STARTED, RUNNING).contains(monitor.getState())) {
                continue;
            }

            // Remove task info from the tasks map one hour after completion/failure/etc.
            long age = System.currentTimeMillis() - monitor.getEndTime();
            if (age > Duration.ofHours(1).toMillis()) {
                log.info("Cleaning up stale export task for project [{}]:",
                        e.getValue().task.getRequest().getProject().getName());
                tasks.remove(e.getKey());
                File exportedFile = e.getValue().task.getMonitor().getExportedFile();
                if (exportedFile.exists()) {
                    try {
                        FileUtils.forceDelete(exportedFile);
                    }
                    catch (IOException ex) {
                        log.error("Unable to clean up stale exported file [{}]:", exportedFile, ex);
                    }
                }
            }
        }
    }

    private static class TaskInfo
    {
        private final Future<?> future;
        private final ProjectExportTask_ImplBase task;

        public TaskInfo(Future<?> aFuture, ProjectExportTask_ImplBase aTask)
        {
            future = aFuture;
            task = aTask;
        }
    }
}
