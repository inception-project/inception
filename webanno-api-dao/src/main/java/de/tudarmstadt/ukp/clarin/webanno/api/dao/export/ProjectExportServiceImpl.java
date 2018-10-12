/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;

@Component
public class ProjectExportServiceImpl
    implements ProjectExportService
{
    public static final String EXPORTED_PROJECT = "exportedproject";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectService projectService;
    
    private final List<ProjectExporter> exportersProxy;
    private List<ProjectExporter> exporters;

    @Autowired
    public ProjectExportServiceImpl(
            @Lazy @Autowired(required = false) List<ProjectExporter> aExporters,
            @Autowired ProjectService aProjectService)
    {
        exportersProxy = aExporters;
        projectService = aProjectService;
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
    public File exportProject(final ProjectExportRequest aRequest)
        throws ProjectExportException
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
    
            ExportedProject exProjekt = exportProject(aRequest, exportTempDir);
                    
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
            
            aRequest.progress = 100;
            
            success = true;
    
            return projectZipFile;
        }
        catch (IOException e) {
            throw new ProjectExportException("Unable to export project", e);
        }
        finally {
            if (!success && exportTempDir != null) {
                try {
                    FileUtils.forceDelete(exportTempDir);
                } catch (IOException e) {
                    log.error(
                            "Unable to delete temporary export directory [" + exportTempDir + "]");
                }
            }
        }
    }
    
    private ExportedProject exportProject(ProjectExportRequest aRequest, File aStage)
        throws ProjectExportException
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
                    initializer.exportData(aRequest, exProject, aStage);
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
        Deque<ProjectExporter> deque = new LinkedList<>(exporters);
        Set<Class<? extends ProjectExporter>> initsSeen = new HashSet<>();
        Set<ProjectExporter> initsDeferred = SetUtils.newIdentityHashSet();
        
        Project project = new Project();
        
        try {
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
}
