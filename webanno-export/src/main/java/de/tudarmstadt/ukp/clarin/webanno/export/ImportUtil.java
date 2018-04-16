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
package de.tudarmstadt.ukp.clarin.webanno.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.JsonImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedMiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * This class contains Utility methods that can be used in Project settings.
 */
public class ImportUtil
{
    public static final String META_INF = "META-INF";
    public static final String SOURCE = "source";
    public static final String TRAIN = "train";
    public static final String ANNOTATION_AS_SERIALISED_CAS = "annotation_ser";
    public static final String CURATION_AS_SERIALISED_CAS = "curation_ser";
    public static final String GUIDELINE = "guideline";
    public static final String EXPORTED_PROJECT = "exportedproject";
    public static final String CONSTRAINTS = "constraints";

    private static final Logger LOG = LoggerFactory.getLogger(ImportUtil.class);

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     * 
     * @param aLineSeparatedTags the line.
     * @return the parsed line.
     */
    public static Map<String, String> getTagSetFromFile(String aLineSeparatedTags)
    {
        Map<String, String> tags = new LinkedHashMap<>();
        StringTokenizer st = new StringTokenizer(aLineSeparatedTags, "\n");
        while (st.hasMoreTokens()) {
            StringTokenizer stTag = new StringTokenizer(st.nextToken(), "\t");
            String tag = stTag.nextToken();
            String description;
            if (stTag.hasMoreTokens()) {
                description = stTag.nextToken();
            }
            else {
                description = tag;
            }
            tags.put(tag.trim(), description);
        }
        return tags;
    }

    /**
     * Check if the zip file is webanno compatible
     * 
     * @param aZipFile the file.
     * @return if it is valid.
     * @throws ZipException if the ZIP file is corrupt.
     * @throws IOException if an I/O error occurs.
     *
     */
    @SuppressWarnings({ "rawtypes" })
    public static boolean isZipValidWebanno(File aZipFile)
        throws IOException
    {

        boolean isZipValidWebanno = false;
        ZipFile zip = new ZipFile(aZipFile);
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(ImportUtil.EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                isZipValidWebanno = true;
                break;
            }
        }
        return isZipValidWebanno;
    }

    /**
     * Remove Invalid characters
     * 
     * @param aName a name.
     * @return a valid name.
     */
    public static String validName(String aName)
    {
        return aName.replace("^", "").replace("/", "").replace("\\", "").replace("&", "")
                .replace("*", "").replace("?", "").replace("+", "").replace("$", "")
                .replace("!", "").replace("[", "").replace("]", "");
    }

    /**
     * Create a {@link TagSet} for the imported project,
     * 
     * @param aProjecct
     *            a project.
     * @param aImportedProjectSetting
     *            the settings.
     * @param aRepository
     *            the repository service.
     * @param aAnnotationService
     *            the annotation service.
     * @return hum?
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Deprecated
    public static Map<String, AnnotationFeature> createLayer(Project aProjecct,
            ExportedProject aImportedProjectSetting, UserDao aRepository,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        User user = aRepository.getCurrentUser();
        List<ExportedTagSet> importedTagSets = aImportedProjectSetting.getTagSets();
        if (aImportedProjectSetting.getVersion() == 0) {
            // this is projects prior to version 2.0
            createV0TagSet(aProjecct, importedTagSets, aAnnotationService, user);
            return new HashMap<>();
        }
        return createV1Layer(aProjecct, aImportedProjectSetting, aAnnotationService, user);
    }

    /**
     * Import tagsets from projects prior to WebAnno 2.0.
     */
    @Deprecated
    private static void createV0TagSet(Project aProject,
            List<ExportedTagSet> importedTagSets,
            AnnotationSchemaService aAnnotationService, User user)
        throws IOException
    {
        List<String> posTags = new ArrayList<>();
        List<String> depTags = new ArrayList<>();
        List<String> neTags = new ArrayList<>();
        List<String> posTagDescriptions = new ArrayList<>();
        List<String> depTagDescriptions = new ArrayList<>();
        List<String> neTagDescriptions = new ArrayList<>();
        List<String> corefTypeTags = new ArrayList<>();
        List<String> corefRelTags = new ArrayList<>();
        for (ExportedTagSet tagSet : importedTagSets) {
            switch (tagSet.getTypeName()) {
            case WebAnnoConst.POS:
                for (ExportedTag tag : tagSet.getTags()) {
                    posTags.add(tag.getName());
                    posTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.DEPENDENCY:
                for (ExportedTag tag : tagSet.getTags()) {
                    depTags.add(tag.getName());
                    depTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.NAMEDENTITY:
                for (ExportedTag tag : tagSet.getTags()) {
                    neTags.add(tag.getName());
                    neTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.COREFRELTYPE:
                for (ExportedTag tag : tagSet.getTags()) {
                    corefTypeTags.add(tag.getName());
                }
                break;
            case WebAnnoConst.COREFERENCE:
                for (ExportedTag tag : tagSet.getTags()) {
                    corefRelTags.add(tag.getName());
                }
                break;
            }
        }
        
        new LegacyProjectInitializer(aAnnotationService).initialize(aProject,
                posTags.toArray(new String[0]), posTagDescriptions.toArray(new String[0]),
                depTags.toArray(new String[0]), depTagDescriptions.toArray(new String[0]),
                neTags.toArray(new String[0]), neTagDescriptions.toArray(new String[0]),
                corefTypeTags.toArray(new String[0]), corefRelTags.toArray(new String[0]));
    }

    @Deprecated
    private static Map<String, AnnotationFeature> createV1Layer(
            Project aProject,
            ExportedProject aImportedProjectSetting,
            AnnotationSchemaService aAnnotationService, User aUser)
        throws IOException
    {
        Map<String, AnnotationFeature> featuresMap = new HashMap<>();
        Map<ExportedAnnotationLayer, AnnotationLayer>
            layersMap = new HashMap<>();
        for (ExportedAnnotationLayer exLayer :
                aImportedProjectSetting.getLayers()) {
            if (aAnnotationService.existsLayer(exLayer.getName(), exLayer.getType(), aProject)) {
                AnnotationLayer layer = aAnnotationService.getLayer(exLayer.getName(), aProject);
                setLayer(aAnnotationService, layer, exLayer, aProject, aUser);
                layersMap.put(exLayer, layer);
                for (ExportedAnnotationFeature exfeature : exLayer.getFeatures()) {
                    if (aAnnotationService.existsFeature(exfeature.getName(), layer)) {
                        AnnotationFeature feature = aAnnotationService.getFeature(
                                exfeature.getName(), layer);
                        setFeature(aAnnotationService, feature, exfeature, aProject, aUser);
                        featuresMap.put(exfeature.getName(), feature);
                        continue;
                    }
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    setFeature(aAnnotationService, feature, exfeature, aProject, aUser);
                    featuresMap.put(exfeature.getName(), feature);
                }
            }
            else {
                AnnotationLayer layer = new AnnotationLayer();
                setLayer(aAnnotationService, layer, exLayer, aProject, aUser);
                layersMap.put(exLayer, layer);
                for (ExportedAnnotationFeature exfeature : exLayer.getFeatures()) {
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    setFeature(aAnnotationService, feature, exfeature, aProject, aUser);
                    featuresMap.put(exfeature.getName(), feature);
                }
            }
        }

        for (ExportedTagSet exTagSet :
                aImportedProjectSetting.getTagSets()) {
            TagSet tagSet = new TagSet();
            createTagSet(tagSet, exTagSet, aProject, aUser, aAnnotationService);
        }

        for (ExportedAnnotationLayer exLayer :
                aImportedProjectSetting.getLayers()) {
            if (exLayer.getAttachType() != null) {
                AnnotationLayer layer = aAnnotationService.getLayer(exLayer.getName(), aProject);
                AnnotationLayer attachLayer = aAnnotationService.getLayer(exLayer.getAttachType()
                        .getName(), aProject);
                layer.setAttachType(attachLayer);
                aAnnotationService.createLayer(layersMap.get(exLayer));
            }
            if (exLayer.getAttachFeature() != null) {
                layersMap.get(exLayer)
                        .setAttachFeature(featuresMap.get(exLayer.getAttachFeature().getName()));
                aAnnotationService.createLayer(layersMap.get(exLayer));
            }

            for (ExportedAnnotationFeature eXFeature : exLayer.getFeatures()) {
                if (eXFeature.getTagSet() != null) {
                    featuresMap.get(eXFeature.getName())
                            .setTagset(
                                    aAnnotationService.getTagSet(eXFeature.getTagSet().getName(),
                                            aProject));
                }
            }
        }
        return featuresMap;
    }

    @Deprecated
    public static void createTagSet(TagSet aTagSet,
            ExportedTagSet aExTagSet, Project aProject,
            User aUser, AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        aTagSet.setCreateTag(aExTagSet.isCreateTag());
        aTagSet.setDescription(aExTagSet.getDescription());
        aTagSet.setLanguage(aExTagSet.getLanguage());
        aTagSet.setName(aExTagSet.getName());
        aTagSet.setProject(aProject);
        aAnnotationService.createTagSet(aTagSet);

        for (ExportedTag exTag : aExTagSet.getTags()) {
            // do not duplicate tag
            if (aAnnotationService.existsTag(exTag.getName(), aTagSet)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setDescription(exTag.getDescription());
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            aAnnotationService.createTag(tag);
        }
    }

    @Deprecated
    public static void setLayer(AnnotationSchemaService aAnnotationService, AnnotationLayer aLayer,
            ExportedAnnotationLayer aExLayer,
            Project aProject, User aUser)
        throws IOException
    {
        aLayer.setAllowStacking(aExLayer.isAllowStacking());
        aLayer.setBuiltIn(aExLayer.isBuiltIn());
        aLayer.setReadonly(aExLayer.isReadonly());
        aLayer.setCrossSentence(aExLayer.isCrossSentence());
        aLayer.setDescription(aExLayer.getDescription());
        aLayer.setEnabled(aExLayer.isEnabled());
        aLayer.setLockToTokenOffset(aExLayer.isLockToTokenOffset());
        aLayer.setMultipleTokens(aExLayer.isMultipleTokens());
        aLayer.setLinkedListBehavior(aExLayer.isLinkedListBehavior());
        aLayer.setUiName(aExLayer.getUiName());
        aLayer.setName(aExLayer.getName());
        aLayer.setProject(aProject);
        aLayer.setType(aExLayer.getType());
        aAnnotationService.createLayer(aLayer);
    }

    @Deprecated
    public static void setFeature(AnnotationSchemaService aAnnotationService,
            AnnotationFeature aFeature,
            ExportedAnnotationFeature aExFeature,
            Project aProject, User aUser)
    {
        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setLayer(aFeature.getLayer());
        boolean isItChainedLayer = aFeature.getLayer().getType().equals(WebAnnoConst.CHAIN_TYPE);
        if (isItChainedLayer && (aExFeature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)
                || aExFeature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE))) {
            aFeature.setType(CAS.TYPE_NAME_STRING);
        }
        else {
            aFeature.setType(aExFeature.getType());
        }
        aFeature.setName(aExFeature.getName());
        aFeature.setRemember(aExFeature.isRemember());
        aFeature.setRequired(aExFeature.isRequired());
        aFeature.setHideUnconstraintFeature(aExFeature.isHideUnconstraintFeature());
        aFeature.setMode(aExFeature.getMultiValueMode());
        aFeature.setLinkMode(aExFeature.getLinkMode());
        aFeature.setLinkTypeName(aExFeature.getLinkTypeName());
        aFeature.setLinkTypeRoleFeatureName(aExFeature.getLinkTypeRoleFeatureName());
        aFeature.setLinkTypeTargetFeatureName(aExFeature.getLinkTypeTargetFeatureName());
        aFeature.setTraits(aExFeature.getTraits());

        aAnnotationService.createFeature(aFeature);
    }

    /**
     * create new {@link Project} from the
     * {@link ExportedProject} model
     * @param aProject the project
     * @param aRepository the repository service.
     * @return the project.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    public static Project createProject(
            ExportedProject aProject,
            ProjectService aRepository)
        throws IOException
    {
        Project project = new Project();
        String projectName = aProject.getName();
        if (aRepository.existsProject(projectName)) {
            projectName = copyProjectName(aRepository, projectName);
        }
        project.setName(projectName);
        project.setDescription(aProject.getDescription());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        project.setMode(StringUtils.lowerCase(aProject.getMode(), Locale.US));
        project.setDisableExport(aProject.isDisableExport());
        project.setCreated(aProject.getCreated());
        project.setUpdated(aProject.getUpdated());
        
        // Set default to LTR on import from old WebAnno versions
        if (aProject.getScriptDirection() == null) {
            project.setScriptDirection(ScriptDirection.LTR);
        }
        else {
            project.setScriptDirection(aProject.getScriptDirection());
        }
        
        aRepository.createProject(project);
        return project;
    }

    /**
     * Get a project name to be used when importing. Use the prefix, copy_of_...+ i to avoid
     * conflicts
     */
    @Deprecated
    private static String copyProjectName(ProjectService aRepository, String aProjectName)
    {
        String projectName = "copy_of_" + aProjectName;
        int i = 1;
        while (true) {
            if (aRepository.existsProject(projectName)) {
                projectName = "copy_of_" + aProjectName + "(" + i + ")";
                i++;
            }
            else {
                return projectName;
            }
        }
    }

    /**
     * Create s {@link SourceDocument} from the exported {@link SourceDocument}
     * 
     * @param aImportedProjectSetting  the exported project.
     * @param aImportedProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    public static void createSourceDocument(
            ExportedProject aImportedProjectSetting,
            Project aImportedProject, DocumentService aRepository)
        throws IOException
    {
        for (ExportedSourceDocument importedSourceDocument : aImportedProjectSetting
                .getSourceDocuments()) {
            SourceDocument sourceDocument = new SourceDocument();
            sourceDocument.setFormat(importedSourceDocument.getFormat());
            sourceDocument.setName(importedSourceDocument.getName());
            sourceDocument.setState(importedSourceDocument.getState());
            sourceDocument.setProject(aImportedProject);
            sourceDocument.setTimestamp(importedSourceDocument.getTimestamp());
            sourceDocument.setSentenceAccessed(importedSourceDocument.getSentenceAccessed());
            sourceDocument.setCreated(importedSourceDocument.getCreated());
            sourceDocument.setUpdated(importedSourceDocument.getUpdated());
          
            aRepository.createSourceDocument(sourceDocument);
        }
    }

    @Deprecated
    public static void createTrainingDocument(
            ExportedProject aImportedProjectSetting,
            Project aImportedProject, AutomationService aRepository, 
            Map<String, AnnotationFeature> aFeatureMap)
        throws IOException
    {
        ExportedTrainingDocument[] trainingDocuments = aImportedProjectSetting
                .getArrayProperty("training_documents", ExportedTrainingDocument.class);
        
        for (ExportedTrainingDocument importedTrainingDocument : trainingDocuments) {
            de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument trainingDocument =
                    new de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument();
            trainingDocument.setFormat(importedTrainingDocument.getFormat());
            trainingDocument.setName(importedTrainingDocument.getName());
            trainingDocument.setState(importedTrainingDocument.getState());
            trainingDocument.setProject(aImportedProject);
            trainingDocument.setTimestamp(importedTrainingDocument.getTimestamp());
            trainingDocument.setSentenceAccessed(importedTrainingDocument.getSentenceAccessed());
            if (importedTrainingDocument.getFeature() != null) {
                trainingDocument.setFeature(
                        aFeatureMap.get(importedTrainingDocument.getFeature().getName()));
            }
            aRepository.createTrainingDocument(trainingDocument);
        }
    }
    
    @Deprecated
    public static void createMiraTemplate(
            ExportedProject aImportedProjectSetting,
            AutomationService aRepository,
            Map<String, AnnotationFeature> aFeatureMaps)
    {
        ExportedMiraTemplate[] templates = aImportedProjectSetting
                .getArrayProperty("mira_templates", ExportedMiraTemplate.class);
        
        for (ExportedMiraTemplate exTemplate : templates) {
            de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate template = 
                    new de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate();
            template.setAnnotateAndRepeat(exTemplate.isAnnotateAndPredict());
            template.setAutomationStarted(false);
            template.setCurrentLayer(exTemplate.isCurrentLayer());
            template.setResult("---");
            template.setTrainFeature(aFeatureMaps.get(exTemplate.getTrainFeature().getName()));
            Set<AnnotationFeature> otherFeatures = new HashSet<>();
            if (exTemplate.getOtherFeatures() != null) {
                for (ExportedAnnotationFeatureReference other : exTemplate.getOtherFeatures()) {
                    otherFeatures.add(aFeatureMaps.get(other.getName()));
                }
                template.setOtherFeatures(otherFeatures);
            }
            aRepository.createTemplate(template);
        }
    }

    /**
     * Create {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument} from exported
     * {@link ExportedAnnotationDocument}
     * 
     * @param aImportedProjectSetting the imported project.
     * @param aImportedProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    public static void createAnnotationDocument(
            ExportedProject aImportedProjectSetting,
            Project aImportedProject, DocumentService aRepository)
        throws IOException
    {
        for (ExportedAnnotationDocument importedAnnotationDocument : aImportedProjectSetting
                .getAnnotationDocuments()) {
            de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument = 
                    new de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument();
            annotationDocument.setName(importedAnnotationDocument.getName());
            annotationDocument.setState(importedAnnotationDocument.getState());
            annotationDocument.setProject(aImportedProject);
            annotationDocument.setUser(importedAnnotationDocument.getUser());
            annotationDocument.setTimestamp(importedAnnotationDocument.getTimestamp());
            annotationDocument.setDocument(aRepository.getSourceDocument(aImportedProject,
                    importedAnnotationDocument.getName()));
            annotationDocument
                    .setSentenceAccessed(importedAnnotationDocument.getSentenceAccessed());
            annotationDocument.setCreated(importedAnnotationDocument.getCreated());
            annotationDocument.setUpdated(importedAnnotationDocument.getUpdated());
            aRepository.createAnnotationDocument(annotationDocument);
        }
    }

    @Deprecated
    public static void createMissingUsers(
            ExportedProject aImportedProjectSetting,
            UserDao aUserDao)
    {
        Set<String> users = new HashSet<>();
        
        for (ExportedProjectPermission importedPermission : aImportedProjectSetting
                .getProjectPermissions()) {
            users.add(importedPermission.getUser());
        }
        
        for (String user : users) {
            if (!aUserDao.exists(user)) {
                User u = new User();
                u.setUsername(user);
                u.setEnabled(false);
                aUserDao.create(u);
            }
        }
    }

    /**
     * copy source document files from the exported source documents
     * @param zip the ZIP file.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static void createSourceDocumentContent(ZipFile zip, Project aProject,
            DocumentService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(SOURCE)) {
                String fileName = FilenameUtils.getName(entryName);
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File sourceFilePath = aRepository.getSourceDocumentFile(sourceDocument);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), sourceFilePath);
                
                LOG.info("Imported content for source document ["
                        + sourceDocument.getId() + "] in project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            }
        }
    }
    
    @Deprecated
    public static void createTrainingDocumentContent(ZipFile zip, Project aProject,
            AutomationService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(TRAIN)) {
                String fileName = FilenameUtils.getName(entryName);
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument trainingDocument =
                        aRepository.getTrainingDocument(aProject, fileName);
                File trainigFilePath = aRepository.getTrainingDocumentFile(trainingDocument);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), trainigFilePath);
                
                LOG.info("Imported content for training document [" + trainingDocument.getId()
                        + "] in project [" + aProject.getName() + "] with id [" + aProject.getId()
                        + "]");
            }
        }
    }

    /**
     * copy annotation documents (serialized CASs) from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static void createAnnotationDocumentContent(ZipFile zip, Project aProject,
            DocumentService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(ANNOTATION_AS_SERIALISED_CAS + "/")) {
                String fileName = entryName.replace(ANNOTATION_AS_SERIALISED_CAS + "/", "");

                if (fileName.trim().isEmpty()) {
                    continue;
                }
                
                // the user annotated the document is file name minus extension (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File annotationFilePath = aRepository.getCasFile(sourceDocument, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
                
                LOG.info("Imported annotation document content for user [" + username
                        + "] for source document [" + sourceDocument.getId() + "] in project ["
                        + aProject.getName() + "] with id [" + aProject.getId() + "]");
            }
        }
    }

    /**
     * Copy curation documents from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static void createCurationDocumentContent(ZipFile zip, Project aProject,
            DocumentService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(CURATION_AS_SERIALISED_CAS)) {
                String fileName = entryName.replace(CURATION_AS_SERIALISED_CAS, "");
                // the user annotated the document is file name minus extension
                // (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File annotationFilePath = aRepository.getCasFile(sourceDocument, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
                
                LOG.info("Imported curation document content for user [" + username
                        + "] for source document [" + sourceDocument.getId() + "] in project ["
                        + aProject.getName() + "] with id [" + aProject.getId() + "]");
            }
        }
    }

    @Deprecated
    public static ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer, AnnotationSchemaService aAnnotationService)
    {
        ExportedAnnotationLayer exLayer = new ExportedAnnotationLayer();
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setBuiltIn(aLayer.isBuiltIn());
        exLayer.setReadonly(aLayer.isReadonly());
        exLayer.setCrossSentence(aLayer.isCrossSentence());
        exLayer.setDescription(aLayer.getDescription());
        exLayer.setEnabled(aLayer.isEnabled());
        exLayer.setLockToTokenOffset(aLayer.isLockToTokenOffset());
        exLayer.setMultipleTokens(aLayer.isMultipleTokens());
        exLayer.setLinkedListBehavior(aLayer.isLinkedListBehavior());
        exLayer.setName(aLayer.getName());
        exLayer.setProjectName(aLayer.getProject().getName());
        exLayer.setType(aLayer.getType());
        exLayer.setUiName(aLayer.getUiName());

        if (aLayerToExLayer != null) {
            aLayerToExLayer.put(aLayer, exLayer);
        }

        List<ExportedAnnotationFeature> exFeatures = new ArrayList<>();
        for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aLayer)) {
            ExportedAnnotationFeature exFeature = new ExportedAnnotationFeature();
            exFeature.setDescription(feature.getDescription());
            exFeature.setEnabled(feature.isEnabled());
            exFeature.setRemember(feature.isRemember());
            exFeature.setRequired(feature.isRequired());
            exFeature.setHideUnconstraintFeature(feature.isHideUnconstraintFeature());
            exFeature.setName(feature.getName());
            exFeature.setProjectName(feature.getProject().getName());
            exFeature.setType(feature.getType());
            exFeature.setUiName(feature.getUiName());
            exFeature.setVisible(feature.isVisible());
            exFeature.setMultiValueMode(feature.getMultiValueMode());
            exFeature.setLinkMode(feature.getLinkMode());
            exFeature.setLinkTypeName(feature.getLinkTypeName());
            exFeature.setLinkTypeRoleFeatureName(feature.getLinkTypeRoleFeatureName());
            exFeature.setLinkTypeTargetFeatureName(feature.getLinkTypeTargetFeatureName());
            exFeature.setTraits(feature.getTraits());
            
            if (feature.getTagset() != null) {
                TagSet tagSet = feature.getTagset();
                ExportedTagSet exTagSet = new ExportedTagSet();
                exTagSet.setDescription(tagSet.getDescription());
                exTagSet.setLanguage(tagSet.getLanguage());
                exTagSet.setName(tagSet.getName());
                exTagSet.setCreateTag(tagSet.isCreateTag());

                List<ExportedTag> exportedTags = new ArrayList<>();
                for (Tag tag : aAnnotationService.listTags(tagSet)) {
                    ExportedTag exTag = new ExportedTag();
                    exTag.setDescription(tag.getDescription());
                    exTag.setName(tag.getName());
                    exportedTags.add(exTag);
                }
                exTagSet.setTags(exportedTags);
                exFeature.setTagSet(exTagSet);
            }
            exFeatures.add(exFeature);
            if (aFeatureToExFeature != null) {
                aFeatureToExFeature.put(feature, exFeature);
            }
        }
        exLayer.setFeatures(exFeatures);
        return exLayer;
    }

    public static String normalizeEntryName(ZipEntry aEntry)
    {
        // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
        String entryName = aEntry.toString();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
       
        return entryName;
    }

    private static TagSet createTagSet(Project project, User user,
            ExportedTagSet importedTagSet,
            AnnotationSchemaService aAnnotationService)
                throws IOException
    {
        String importedTagSetName = importedTagSet.getName();
        if (aAnnotationService.existsTagSet(importedTagSetName, project)) {
            // aAnnotationService.removeTagSet(aAnnotationService.getTagSet(
            // importedTagSet.getName(), project));
            // Rename Imported TagSet instead of deleting the old one.
            importedTagSetName = JsonImportUtil.copyTagSetName(aAnnotationService,
                    importedTagSetName, project);
        }

        TagSet newTagSet = new TagSet();
        newTagSet.setDescription(importedTagSet.getDescription());
        newTagSet.setName(importedTagSetName);
        newTagSet.setLanguage(importedTagSet.getLanguage());
        newTagSet.setProject(project);
        aAnnotationService.createTagSet(newTagSet);
        for (ExportedTag tag : importedTagSet.getTags()) {
            Tag newTag = new Tag();
            newTag.setDescription(tag.getDescription());
            newTag.setName(tag.getName());
            newTag.setTagSet(newTagSet);
            aAnnotationService.createTag(newTag);
        }
        
        return newTagSet;
    }
}
