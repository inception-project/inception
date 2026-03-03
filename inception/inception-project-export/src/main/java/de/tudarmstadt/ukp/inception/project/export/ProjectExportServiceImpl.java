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
package de.tudarmstadt.ukp.inception.project.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.NOT_STARTED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.RUNNING;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ConcurrentReferenceHashMap;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.event.PrepareProjectExportEvent;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.model.ProjectExportTask;
import de.tudarmstadt.ukp.inception.project.export.task.backup.BackupProjectExportTask;
import de.tudarmstadt.ukp.inception.project.export.task.curated.CuratedDocumentsProjectExportRequest;
import de.tudarmstadt.ukp.inception.project.export.task.curated.CuratedDocumentsProjectExportTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectExportServiceAutoConfiguration#projectExportService}.
 * </p>
 */
public class ProjectExportServiceImpl
    implements ProjectExportService, DisposableBean
{
    public static final String EXPORTED_PROJECT = "exportedproject";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    private static final Duration STALE_EXPORT_EXPIRY = Duration.ofMinutes(30);

    private final Map<ProjectExportTaskHandle, TaskInfo> tasks = new ConcurrentReferenceHashMap<>();
    private final ProjectService projectService;
    private final ExecutorService taskExecutorService;
    private final ScheduledExecutorService cleaningScheduler;
    private final ApplicationContext applicationContext;
    private final SchedulingService schedulingService;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final List<ProjectExporter> exportersProxy;
    private List<ProjectExporter> exporters;

    @Autowired
    public ProjectExportServiceImpl(ApplicationContext aApplicationContext,
            @Lazy @Autowired(required = false) List<ProjectExporter> aExporters,
            ProjectService aProjectService, SchedulingService aSchedulingService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        applicationContext = aApplicationContext;
        exportersProxy = aExporters;
        projectService = aProjectService;
        schedulingService = aSchedulingService;
        applicationEventPublisher = aApplicationEventPublisher;

        taskExecutorService = Executors.newFixedThreadPool(4);

        cleaningScheduler = Executors.newScheduledThreadPool(1);
        cleaningScheduler.scheduleAtFixedRate(this::cleanUp, CLEANUP_INTERVAL.toMillis(),
                CLEANUP_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
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
                    LOG.debug("Found project exporter: {}",
                            ClassUtils.getAbbreviatedName(init.getClass(), 20));
                }
                else {
                    throw new IllegalStateException("There cannot be more than once instance "
                            + "of each project exporter class! Duplicate instance of class: "
                            + init.getClass());
                }
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] project exporters", exps.size());

        exporters = unmodifiableList(exps);
    }

    @Override
    @Transactional
    public File exportProject(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException, IOException, InterruptedException
    {
        var filename = aRequest.getProject().getSlug();
        // temp-file prefix must be at least 3 chars
        filename = StringUtils.rightPad(filename, 3, "_");
        var projectZipFile = File.createTempFile(filename, ".zip");
        var success = false;
        try {
            exportProject(aRequest, aMonitor, projectZipFile);
            success = true;
            return projectZipFile;
        }
        finally {
            if (!success) {
                FileUtils.forceDelete(projectZipFile);
            }
        }
    }

    private void exportProject(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            File aProjectZipFile)
        throws ProjectExportException, IOException, InterruptedException
    {
        var success = false;
        File exportTempDir = null;
        try (var logCtx = withProjectLogger(aRequest.getProject())) {
            // Directory to store source documents and annotation documents

            try (var zos = new ZipOutputStream(new FileOutputStream(aProjectZipFile));) {
                var exProjekt = exportProjectToZip(aRequest, aMonitor, zos);
                writeProjectJson(zos, exProjekt);
            }

            success = true;
        }
        finally {
            if (!success && exportTempDir != null) {
                try {
                    FileUtils.forceDelete(exportTempDir);
                }
                catch (IOException e) {
                    aMonitor.addMessage(LogMessage.error(this,
                            "Unable to delete temporary export directory [%s]", exportTempDir));
                    LOG.error("Unable to delete temporary export directory [{}]", exportTempDir);
                }
            }
        }
    }

    private void writeProjectJson(ZipOutputStream aZOs, ExportedProject aExProjekt)
        throws IOException
    {
        var zipEntry = new ZipEntry(EXPORTED_PROJECT + ".json");
        aZOs.putNextEntry(zipEntry);

        try {
            var jsonBytes = JSONUtil.toPrettyJsonString(aExProjekt).getBytes();
            aZOs.write(jsonBytes, 0, jsonBytes.length);
        }
        finally {
            aZOs.closeEntry();
        }
    }

    private ExportedProject exportProjectToZip(FullProjectExportRequest aRequest,
            ProjectExportTaskMonitor aMonitor, ZipOutputStream aZip)
        throws ProjectExportException, IOException, InterruptedException
    {
        applicationEventPublisher
                .publishEvent(new PrepareProjectExportEvent(this, aRequest.getProject()));

        var deque = new LinkedList<>(exporters);
        var exportersSeen = new HashSet<Class<? extends ProjectExporter>>();
        Set<ProjectExporter> exportersDeferred = SetUtils.newIdentityHashSet();

        var exProject = new ExportedProject();
        exProject.setName(aRequest.getProject().getName());
        exProject.setSlug(aRequest.getProject().getSlug());

        // Exporting without suspension is probably ok
        // try (var ctx = schedulingService.whileSuspended(aRequest.getProject())) {
        try {
            while (!deque.isEmpty()) {
                var exporter = deque.pop();

                if (exportersDeferred.contains(exporter)) {
                    throw new IllegalStateException("Circular exporter dependencies in "
                            + exportersDeferred + " via " + exporter);
                }

                if (exportersSeen.containsAll(exporter.getExportDependencies())) {
                    LOG.debug("Applying project exporter: {}", exporter);
                    exporter.exportData(aRequest, aMonitor, exProject, aZip);
                    exportersSeen.add(exporter.getClass());
                    exportersDeferred.clear();
                }
                else {
                    LOG.debug(
                            "Deferring project exporter as dependencies are not yet fulfilled: [{}]",
                            exporter);
                    deque.add(exporter);
                    exportersDeferred.add(exporter);
                }
            }
        }
        catch (ProjectExportException e) {
            throw e;
        }
        catch (InterruptedException | IOException e) {
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

        var deque = new LinkedList<ProjectExporter>(exporters);
        var initsSeen = new HashSet<Class<? extends ProjectExporter>>();
        Set<ProjectExporter> initsDeferred = SetUtils.newIdentityHashSet();

        var project = new Project();

        try (var ctx = schedulingService.whileSuspended(project)) {
            var exProject = loadExportedProject(aZip);

            project.setName(exProject.getName());

            // Old projects do not have a slug, so we derive one from the project name
            var slug = exProject.getSlug();
            if (isBlank(slug)) {
                slug = projectService.deriveSlugFromName(exProject.getName());
            }

            // If the slug is already taken, generate a unique one
            project.setSlug(projectService.deriveUniqueSlug(slug));

            // Initial saving of the project
            projectService.createProject(project);

            // Apply the importers
            while (!deque.isEmpty()) {
                var importer = deque.pop();

                if (initsDeferred.contains(importer)) {
                    throw new IllegalStateException("Circular initializer dependencies in "
                            + initsDeferred + " via " + importer);
                }

                if (initsSeen.containsAll(importer.getImportDependencies())) {
                    LOG.debug("Applying project importer: {}", importer);
                    importer.importData(aRequest, project, exProject, aZip);
                    initsSeen.add(importer.getClass());
                    initsDeferred.clear();
                }
                else {
                    LOG.debug(
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

        LOG.info("Imported project {} ({})", project,
                formatDurationWords(currentTimeMillis() - start, true, true));

        return project;
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

        if (projectSettingsEntry == null) {
            throw new IOException("Unable to locate JSON file describing the project");
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
    public ProjectExportTaskHandle startProjectExportTask(FullProjectExportRequest aRequest,
            String aUsername)
    {
        var task = new BackupProjectExportTask(aRequest, aUsername);

        return startTask(task);
    }

    @Override
    public ProjectExportTaskHandle startProjectExportCuratedDocumentsTask(
            FullProjectExportRequest aRequest, String aUsername)
    {
        var request = new CuratedDocumentsProjectExportRequest(aRequest.getProject());
        request.setFormat(aRequest.getFormat());
        request.setIncludeInProgress(aRequest.isIncludeInProgress());

        var task = new CuratedDocumentsProjectExportTask(request, aUsername);
        return startTask(task);
    }

    @Override
    public ProjectExportTaskHandle startTask(ProjectExportTask<?> aTask)
    {
        ProjectExportTaskHandle handle = aTask.getHandle();

        // This autowires the task fields manually.
        var factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(aTask);
        factory.initializeBean(aTask, "transientTask");

        tasks.put(handle, new TaskInfo(taskExecutorService.submit(aTask), aTask));

        return handle;
    }

    @Override
    public ProjectExportRequest_ImplBase getExportRequest(ProjectExportTaskHandle aHandle)
    {
        var task = tasks.get(aHandle);

        if (task == null) {
            return null;
        }

        return task.task.getRequest();
    }

    @Override
    public ProjectExportTaskMonitor getTaskMonitor(ProjectExportTaskHandle aHandle)
    {
        var task = tasks.get(aHandle);

        if (task == null) {
            return null;
        }

        return task.task.getMonitor();
    }

    @Override
    public List<ProjectExportTask<?>> listRunningExportTasks(Project aProject)
    {
        return tasks.values().stream() //
                .filter(taskInfo -> aProject.equals(taskInfo.task.getRequest().getProject())) //
                .filter(taskInfo -> !taskInfo.future.isCancelled()) //
                .map(taskInfo -> taskInfo.task) //
                .collect(toList());
    }

    @Override
    public boolean cancelTask(ProjectExportTaskHandle aHandle)
    {
        var task = tasks.get(aHandle);

        if (task == null) {
            return false;
        }

        boolean cancelled = task.future.cancel(true);

        if (cancelled) {
            LOG.debug("Cancelled running export");
        }
        else {
            LOG.debug("Cancelled completed export");
        }

        File exportedFile = task.task.getMonitor().getExportedFile();
        if (exportedFile != null && exportedFile.exists()) {
            LOG.debug("Deleted exported file {}", exportedFile);
            try {
                FileUtils.forceDelete(exportedFile);
            }
            catch (IOException ex) {
                LOG.error("Unable to clean up cancelled exported file [{}]:", exportedFile, ex);
            }
        }

        tasks.remove(aHandle);
        task.task.destroy();

        return cancelled;
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
            if (age > STALE_EXPORT_EXPIRY.toMillis()) {
                LOG.info("Cleaning up stale export task for project [{}]:",
                        e.getValue().task.getRequest().getProject().getName());
                tasks.remove(e.getKey());
                e.getValue().task.destroy();
                File exportedFile = e.getValue().task.getMonitor().getExportedFile();
                if (exportedFile.exists()) {
                    try {
                        FileUtils.forceDelete(exportedFile);
                    }
                    catch (IOException ex) {
                        LOG.error("Unable to clean up stale exported file [{}]:", exportedFile, ex);
                    }
                }
            }
        }
    }

    private static class TaskInfo
    {
        private final Future<?> future;
        private final ProjectExportTask<?> task;

        public TaskInfo(Future<?> aFuture, ProjectExportTask<?> aTask)
        {
            future = aFuture;
            task = aTask;
        }
    }
}
