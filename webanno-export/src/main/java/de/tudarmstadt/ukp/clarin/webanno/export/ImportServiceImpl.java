/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAware;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAwareRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Component(ImportService.SERVICE_NAME)
public class ImportServiceImpl implements ImportService
{
    private @Resource AnnotationSchemaService annotationService;
    private @Resource AutomationService automationService;
    private @Resource DocumentService documentService;
    private @Resource ProjectService projectService;
    private @Resource ConstraintsService constraintsService;
    private @Resource UserDao userRepository;
    private @Resource ProjectLifecycleAwareRegistry projectLifecycleAwareRegistry;
    
    @Override
    public Project importProject(File aProjectFile, boolean aGenerateUsers) throws Exception
    {
        Project importedProject = new Project();
        ZipFile zip = new ZipFile(aProjectFile);
        InputStream projectInputStream = null;
        for (Enumeration<? extends ZipEntry> zipEnumerate = zip.entries(); zipEnumerate
                .hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(ImportUtil.EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                projectInputStream = zip.getInputStream(entry);
                break;
            }
        }

        // Load the project model from the JSON file
        String text = IOUtils.toString(projectInputStream, "UTF-8");
        de.tudarmstadt.ukp.clarin.webanno.export.model.Project importedProjectSetting = JSONUtil
                .getJsonConverter().getObjectMapper()
                .readValue(text, de.tudarmstadt.ukp.clarin.webanno.export.model.Project.class);

        // Import the project itself
        importedProject = ImportUtil.createProject(importedProjectSetting, projectService);

        // Import additional project things
        projectService.onProjectImport(zip, importedProjectSetting, importedProject);

        // Import missing users
        if (aGenerateUsers) {
            ImportUtil.createMissingUsers(importedProjectSetting, userRepository);
        }

        // Notify all relevant service so that they can initialize themselves for the given
        // project
        for (ProjectLifecycleAware bean : projectLifecycleAwareRegistry.getBeans()) {
            try {
                bean.onProjectImport(zip, importedProjectSetting, importedProject);
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        // Import layers
        Map<String, AnnotationFeature> featuresMap = ImportUtil.createLayer(importedProject,
                importedProjectSetting, userRepository, annotationService);
        /*
         * for (TagSet tagset : importedProjectSetting.getTagSets()) {
         * ImportUtil.createTagset(importedProject, tagset, projectRepository, annotationService); }
         */

        // Import source document
        ImportUtil.createSourceDocument(importedProjectSetting, importedProject, documentService);

        // Import Training document
        ImportUtil.createTrainingDocument(importedProjectSetting, importedProject,
                automationService, featuresMap);
        // Import source document content
        ImportUtil.createSourceDocumentContent(zip, importedProject, documentService);
        // Import training document content
        ImportUtil.createTrainingDocumentContent(zip, importedProject, automationService);

        // Import automation settings
        ImportUtil.createMiraTemplate(importedProjectSetting, automationService, featuresMap);

        // Import annotation document content
        ImportUtil.createAnnotationDocument(importedProjectSetting, importedProject,
                documentService);
        // Import annotation document content
        ImportUtil.createAnnotationDocumentContent(zip, importedProject, documentService);

        // Import curation document content
        ImportUtil.createCurationDocumentContent(zip, importedProject, documentService);

        return importedProject;
    }
}
