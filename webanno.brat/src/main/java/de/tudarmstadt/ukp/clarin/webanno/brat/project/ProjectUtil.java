/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.persistence.NoResultException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationDocument;

/**
 * This class contains Utility methods that can be used in Project settings
 *
 * @author Seid Muhie Yimam
 *
 */
public class ProjectUtil
{

    private static MappingJacksonHttpMessageConverter jsonConverter;

    private static final String META_INF = "/META-INF";
    private static final String SOURCE = "/source";
    private static final String ANNOTATION_AS_SERIALISED_CAS = "/annotation_ser/";
    private static final String CURATION_AS_SERIALISED_CAS = "/curation_ser";
    private static final String GUIDELINE = "/guideline";
    private static final String LOG_DIR = "/log";
    public static final String EXPORTED_PROJECT = "exportedproject";

    public static void setJsonConverter(MappingJacksonHttpMessageConverter aJsonConverter)
    {
        jsonConverter = aJsonConverter;
    }

    private static final Log LOG = LogFactory.getLog(ProjectUtil.class);

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     *
     */
    public static Map<String, String> getTagSetFromFile(String aLineSeparatedTags)
    {
        Map<String, String> tags = new LinkedHashMap<String, String>();
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
     * IS user super Admin
     */
    public static boolean isSuperAdmin(RepositoryService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.listAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * Determine if the User is allowed to update a project
     *
     * @param aProject
     * @return
     */
    public static boolean isProjectAdmin(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.listAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean projectAdmin = false;
        if (!roleAdmin) {

            try {
                List<ProjectPermission> permissionLevels = aProjectRepository
                        .listProjectPermisionLevel(aUser, aProject);
                for (ProjectPermission permissionLevel : permissionLevels) {
                    if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                            PermissionLevel.ADMIN.getName())) {
                        projectAdmin = true;
                        break;
                    }
                }
            }
            catch (NoResultException ex) {
                LOG.info("No permision is given to this user " + ex);
            }
        }

        return (projectAdmin || roleAdmin);
    }

    /**
     * Determine if the User is a curator or not
     *
     * @param aProject
     * @return
     */
    public static boolean isCurator(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.listAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean curator = false;
        if (!roleAdmin) {

            try {
                List<ProjectPermission> permissionLevels = aProjectRepository
                        .listProjectPermisionLevel(aUser, aProject);
                for (ProjectPermission permissionLevel : permissionLevels) {
                    if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                            PermissionLevel.CURATOR.getName())) {
                        curator = true;
                        break;
                    }
                }
            }
            catch (NoResultException ex) {
                LOG.info("No permision is given to this user " + ex);
            }
        }

        return (curator || roleAdmin);
    }

    /**
     * Determine if the User is member of a project
     *
     * @param aProject
     * @return
     */
    public static boolean isMember(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.listAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean user = false;
        if (!roleAdmin) {

            try {
                List<ProjectPermission> permissionLevels = aProjectRepository
                        .listProjectPermisionLevel(aUser, aProject);
                for (ProjectPermission permissionLevel : permissionLevels) {
                    if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                            PermissionLevel.USER.getName())) {
                        user = true;
                        break;
                    }
                }
            }

            catch (NoResultException ex) {
                LOG.info("No permision is given to this user " + ex);
            }
        }

        return (user || roleAdmin);
    }

    /**
     * Convert Java objects into JSON format and write it to a file
     *
     * @param aObject
     * @param aFile
     * @throws IOException
     */

    public static void generateJson(Object aObject, File aFile)
        throws IOException
    {
        StringWriter out = new StringWriter();

        JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                .createJsonGenerator(out);

        jsonGenerator.writeObject(aObject);
        FileUtils.writeStringToFile(aFile, out.toString());
    }

    /**
     * Set annotation preferences of users for a given project such as window size, annotation
     * layers,... reading from the file system.
     *
     * @param aUsername
     *            The {@link User} for whom we need to read the preference (preferences are stored
     *            per user)
     * @param aBModel
     *            The {@link BratAnnotatorModel} that will be populated with preferences from the
     *            file
     * @param aMode
     */
    public static void setAnnotationPreference(String aUsername,
            RepositoryService aRepositoryService, AnnotationService aAnnotationService,
            BratAnnotatorModel aBModel, Mode aMode)
        throws BeansException, FileNotFoundException, IOException
    {
        AnnotationPreference preference = new AnnotationPreference();
        BeanWrapper wrapper = new BeanWrapperImpl(preference);
        // get annotation preference from file system
        try {
            for (Entry<Object, Object> entry : aRepositoryService.loadUserSettings(aUsername,
                    aBModel.getProject()).entrySet()) {
                String property = entry.getKey().toString();
                int index = property.lastIndexOf(".");
                String propertyName = property.substring(index + 1);
                String mode = property.substring(0, index);
                if (wrapper.isWritableProperty(propertyName) && mode.equals(aMode.getName())) {

                    if (AnnotationPreference.class.getDeclaredField(propertyName).getGenericType() instanceof ParameterizedType) {
                        List<String> value = Arrays.asList(StringUtils.replaceChars(
                                entry.getValue().toString(), "[]", "").split(","));
                        if (!value.get(0).equals("")) {
                            wrapper.setPropertyValue(propertyName, value);
                        }
                    }
                    else {
                        wrapper.setPropertyValue(propertyName, entry.getValue());
                    }
                }
            }
            aBModel.setWindowSize(preference.getWindowSize());
            aBModel.setScrollPage(preference.isScrollPage());
            aBModel.setStaticColor(preference.isStaticColor());

            // Get tagset using the id, from the properties file
            aBModel.getAnnotationLayers().clear();
            if (preference.getAnnotationLayers() != null) {
                for (Long id : preference.getAnnotationLayers()) {
                    aBModel.getAnnotationLayers().add(aAnnotationService.getTagSet(id));
                }
            }
        }
        // no preference found
        catch (Exception e) {

            // disable corefernce annotation for correction/curation pages for 0.4.0 release
            List<TagSet> tagSets = aAnnotationService.listTagSets(aBModel.getProject());
            List<TagSet> corefTagSets = new ArrayList<TagSet>();
            List<TagSet> noFeatureTagSet = new ArrayList<TagSet>();
            for (TagSet tagSet : tagSets) {
                if (tagSet.getLayer() == null || tagSet.getFeature() == null) {
                    noFeatureTagSet.add(tagSet);
                }
                else if (tagSet.getLayer().getType().equals("chain")) {
                    corefTagSets.add(tagSet);
                }
            }

            if (aMode.equals(Mode.CORRECTION) || aMode.equals(Mode.AUTOMATION)
                    || aMode.equals(Mode.CURATION)) {
                tagSets.removeAll(corefTagSets);
            }
            tagSets.remove(noFeatureTagSet);
            aBModel.setAnnotationLayers(new HashSet<TagSet>(tagSets));
            /*
             * abAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(aAnnotationService
             * .listTagSets(abAnnotatorModel.getProject())));
             */
        }
    }

    // The magic bytes for ZIP
    // see http://notepad2.blogspot.de/2012/07/java-detect-if-stream-or-file-is-zip.html
    private static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

    /**
     * check if the {@link InputStream} provided is a zip file
     */
    public static boolean isZipStream(InputStream in)
    {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in);
        }
        boolean isZip = true;
        try {
            in.mark(MAGIC.length);
            for (byte element : MAGIC) {
                if (element != (byte) in.read()) {
                    isZip = false;
                    break;
                }
            }
            in.reset();
        }
        catch (IOException e) {
            isZip = false;
        }
        return isZip;
    }

    /**
     * Check if the zip file is webanno compatible
     *
     */
    @SuppressWarnings({ "resource", "rawtypes" })
    public static boolean isZipValidWebanno(File aZipFile)
        throws ZipException, IOException
    {

        boolean isZipValidWebanno = false;
        ZipFile zip = new ZipFile(aZipFile);
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(ProjectUtil.EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                isZipValidWebanno = true;
                break;
            }
        }
        return isZipValidWebanno;
    }

    /**
     * Check if the name is valid, SPecial characters are not allowed as a project/user name as it
     * will conflict with file naming system
     */
    public static boolean isNameValid(String aName)
    {
        if (aName.contains("^") || aName.contains("/") || aName.contains("\\")
                || aName.contains("&") || aName.contains("*") || aName.contains("?")
                || aName.contains("+") || aName.contains("$") || aName.contains("!")
                || aName.contains("[") || aName.contains("]")) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Remove Invalid characters
     */
    public static String validName(String aName)
    {
        return aName.replace("^", "").replace("/", "").replace("\\", "").replace("&", "")
                .replace("*", "").replace("?", "").replace("+", "").replace("$", "")
                .replace("!", "").replace("[", "").replace("]", "");
    }

    /**
     * Create a {@link TagSet} for the imported project,
     */
    public static void createTagset(Project aProjecct, int aVersion,
            List<de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet> importedTagSet,
            RepositoryService aRepository, AnnotationService aAnnotationService)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        AnnotationLayer type = null;

        if (aVersion < 2) {// this is projects prio to version 2.0

            List<String> posTags = new ArrayList<String>();
            List<String> depTags = new ArrayList<String>();
            List<String> neTags = new ArrayList<String>();
            List<String> posTagDescriptions = new ArrayList<String>();
            List<String> depTagDescriptions = new ArrayList<String>();
            List<String> neTagDescriptions = new ArrayList<String>();
            List<String> corefTypeTags = new ArrayList<String>();
            List<String> corefRelTags = new ArrayList<String>();
            for (de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet tagSet : importedTagSet) {
                if (tagSet.getTypeName().equals(WebAnnoConst.POS)) {
                    for (de.tudarmstadt.ukp.clarin.webanno.model.export.Tag tag : tagSet.getTags()) {
                        posTags.add(tag.getName());
                        posTagDescriptions.add(tag.getDescription());
                    }
                }
                else if (tagSet.getTypeName().equals(WebAnnoConst.DEPENDENCY)) {
                    for (de.tudarmstadt.ukp.clarin.webanno.model.export.Tag tag : tagSet.getTags()) {
                        depTags.add(tag.getName());
                        depTagDescriptions.add(tag.getDescription());
                    }
                }
                else if (tagSet.getTypeName().equals(WebAnnoConst.NAMEDENTITY)) {
                    for (de.tudarmstadt.ukp.clarin.webanno.model.export.Tag tag : tagSet.getTags()) {
                        neTags.add(tag.getName());
                        neTagDescriptions.add(tag.getDescription());
                    }
                }
                else if (tagSet.getTypeName().equals(WebAnnoConst.COREFRELTYPE)) {
                    for (de.tudarmstadt.ukp.clarin.webanno.model.export.Tag tag : tagSet.getTags()) {
                        corefTypeTags.add(tag.getName());
                    }
                }
                else if (tagSet.getTypeName().equals(WebAnnoConst.COREFERENCE)) {
                    for (de.tudarmstadt.ukp.clarin.webanno.model.export.Tag tag : tagSet.getTags()) {
                        corefRelTags.add(tag.getName());
                    }
                }
            }

            aAnnotationService.initializeTypesForProject(aProjecct, user,
                    posTags.toArray(new String[0]), posTagDescriptions.toArray(new String[0]),
                    depTags.toArray(new String[0]), depTagDescriptions.toArray(new String[0]),
                    neTags.toArray(new String[0]), neTagDescriptions.toArray(new String[0]),
                    corefTypeTags.toArray(new String[0]), corefRelTags.toArray(new String[0]));
        }

        /*
         * if (!aAnnotationService.existsType(importedTagSet.getTypeName(),
         * importedTagSet.getType())) { type = new AnnotationType();
         * type.setDescription(importedTagSet.getTypeDescription());
         * type.setName(importedTagSet.getTypeName()); type.setType(importedTagSet.getType());
         * aAnnotationService.createType(type, user); } else { type =
         * aAnnotationService.getType(importedTagSet.getTypeName(), importedTagSet.getType()); }
         *
         * if (importedTagSet != null) {
         *
         * de.tudarmstadt.ukp.clarin.webanno.model.TagSet newTagSet = new
         * de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
         * newTagSet.setDescription(importedTagSet.getDescription());
         * newTagSet.setName(importedTagSet.getName());
         * newTagSet.setLanguage(importedTagSet.getLanguage()); newTagSet.setProject(aProjecct);
         * newTagSet.setLayer(type); aAnnotationService.createTagSet(newTagSet, user); for (
         * de.tudarmstadt.ukp.clarin.webanno.model.export.Tag tag : importedTagSet.getTags()) { Tag
         * newTag = new Tag(); newTag.setDescription(tag.getDescription());
         * newTag.setName(tag.getName()); newTag.setTagSet(newTagSet);
         * aAnnotationService.createTag(newTag, user); } }
         */
    }

    /**
     * create new {@link Project} from the
     * {@link de.tudarmstadt.ukp.clarin.webanno.model.export.Project} model
     *
     * @throws IOException
     */
    public static Project createProject(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        Project project = new Project();
        String projectName = aProject.getName();
        if (aRepository.existsProject(projectName)) {
            projectName = copyProjectName(aRepository, projectName);
        }
        project.setName(projectName);
        project.setDescription(aProject.getDescription());
        project.setMode(aProject.getMode());
        aRepository.createProject(project, user);
        return project;
    }

    /**
     * Get a project name to be used when importing. Use the prefix, copy_of_...+ i to avoid
     * conflicts
     *
     * @return
     */
    private static String copyProjectName(RepositoryService aRepository, String aProjectName)
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
     */
    public static void createSourceDocument(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            Project aImportedProject, RepositoryService aRepository)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        for (de.tudarmstadt.ukp.clarin.webanno.model.export.SourceDocument importedSourceDocument : aImportedProjectSetting
                .getSourceDocuments()) {
            de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = new de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument();
            sourceDocument.setFormat(importedSourceDocument.getFormat());
            sourceDocument.setName(importedSourceDocument.getName());
            sourceDocument.setState(importedSourceDocument.getState());
            sourceDocument.setProject(aImportedProject);
            sourceDocument.setTimestamp(importedSourceDocument.getTimestamp());
            aRepository.createSourceDocument(sourceDocument, user);
        }
    }

    /**
     * Create {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument} from exported
     * {@link AnnotationDocument}
     */
    public static void createAnnotationDocument(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            Project aImportedProject, RepositoryService aRepository)
        throws IOException
    {
        for (AnnotationDocument importedAnnotationDocument : aImportedProjectSetting
                .getAnnotationDocuments()) {
            de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument = new de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument();
            annotationDocument.setName(importedAnnotationDocument.getName());
            annotationDocument.setState(importedAnnotationDocument.getState());
            annotationDocument.setProject(aImportedProject);
            annotationDocument.setUser(importedAnnotationDocument.getUser());
            annotationDocument.setTimestamp(importedAnnotationDocument.getTimestamp());
            annotationDocument.setDocument(aRepository.getSourceDocument(aImportedProject,
                    importedAnnotationDocument.getName()));
            aRepository.createAnnotationDocument(annotationDocument);
        }
    }

    /**
     * Create {@link ProjectPermission} from the exported
     * {@link de.tudarmstadt.ukp.clarin.webanno.model.export.ProjectPermission}
     */
    public static void createProjectPermission(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            Project aImportedProject, RepositoryService aRepository)
        throws IOException
    {
        for (de.tudarmstadt.ukp.clarin.webanno.model.export.ProjectPermission importedPermission : aImportedProjectSetting
                .getProjectPermissions()) {
            de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission = new de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission();
            permission.setLevel(importedPermission.getLevel());
            permission.setProject(aImportedProject);
            permission.setUser(importedPermission.getUser());
            aRepository.createProjectPermission(permission);
        }
    }

    /**
     * copy source document files from the exported source documents
     */
    @SuppressWarnings("rawtypes")
    public static void createSourceDocumentContent(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(SOURCE)) {
                String fileName = entry.toString().replace(SOURCE, "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File sourceFilePath = aRepository.exportSourceDocument(sourceDocument);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), sourceFilePath);
            }
        }
    }

    /**
     * copy annotation documents (serialized CASs) from the exported project
     */
    @SuppressWarnings("rawtypes")
    public static void createAnnotationDocumentContent(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(ANNOTATION_AS_SERIALISED_CAS)) {
                String fileName = entry.toString().replace(ANNOTATION_AS_SERIALISED_CAS, "");

                // the user annotated the document is file name minus extension (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File annotationFilePath = aRepository.exportserializedCas(sourceDocument, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
            }
        }
    }

    /**
     * Copy curation documents from the exported project
     */
    @SuppressWarnings("rawtypes")
    public static void createCurationDocumentContent(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(CURATION_AS_SERIALISED_CAS)) {
                String fileName = entry.toString().replace(CURATION_AS_SERIALISED_CAS, "");

                // the user annotated the document is file name minus extension (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File annotationFilePath = aRepository.exportserializedCas(sourceDocument, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
            }
        }
    }

    /**
     * copy guidelines from the exported project
     */
    @SuppressWarnings("rawtypes")
    public static void createProjectGuideline(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(GUIDELINE)) {
                File guidelineDir = aRepository.exportGuidelines(aProject);
                FileUtils.forceMkdir(guidelineDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(guidelineDir,
                        FilenameUtils.getName(entry.getName())));
            }
        }
    }

    /**
     * copy Project META_INF from the exported project
     */
    @SuppressWarnings("rawtypes")
    public static void createProjectMetaInf(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(META_INF)) {
                File metaInfDir = new File(aRepository.exportProjectMetaInf(aProject),
                        FilenameUtils.getPath(entry.getName().replace(META_INF, "")));
                // where the file reside in the META-INF/... directory
                FileUtils.forceMkdir(metaInfDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(metaInfDir,
                        FilenameUtils.getName(entry.getName())));
            }
        }
    }

    /**
     * copy project log files from the exported project
     */
    @SuppressWarnings("rawtypes")
    public static void createProjectLog(ZipFile zip, Project aProject, RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().startsWith(LOG_DIR)) {
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry),
                        aRepository.exportProjectLog(aProject));
            }
        }
    }

    /**
     * Return true if there exist at least one annotation document FINISHED for annotation for this
     * {@link SourceDocument}
     */
    public static boolean existFinishedDocument(
            de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument aSourceDocument, User aUser,
            RepositoryService aRepository, Project aProject)
    {
        List<de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument> annotationDocuments = aRepository
                .listAnnotationDocuments(aSourceDocument);
        boolean finishedAnnotationDocumentExist = false;
        for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocumentExist = true;
                break;
            }
        }
        return finishedAnnotationDocumentExist;

    }

    public static void savePreference(BratAnnotatorModel aBModel, RepositoryService aRepository)
        throws FileNotFoundException, IOException
    {
        AnnotationPreference preference = new AnnotationPreference();
        preference.setScrollPage(aBModel.isScrollPage());
        preference.setWindowSize(aBModel.getWindowSize());
        preference.setStaticColor(aBModel.isStaticColor());
        ArrayList<Long> layers = new ArrayList<Long>();

        for (TagSet tagset : aBModel.getAnnotationLayers()) {
            layers.add(tagset.getId());
        }
        preference.setAnnotationLayers(layers);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        aRepository.saveUserSettings(username, aBModel.getProject(), aBModel.getMode(), preference);
    }
}
