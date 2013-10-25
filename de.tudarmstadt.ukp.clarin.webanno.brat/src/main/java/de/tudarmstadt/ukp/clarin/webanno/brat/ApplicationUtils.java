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
package de.tudarmstadt.ukp.clarin.webanno.brat;

import static org.uimafit.util.JCasUtil.select;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.uima.jcas.JCas;
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
import de.tudarmstadt.ukp.clarin.webanno.export.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class contains Utility methods that can be used application wide
 *
 * @author Seid Muhie Yimam
 *
 */
public class ApplicationUtils
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

    private static final Log LOG = LogFactory.getLog(ApplicationUtils.class);

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

    public static void generateJson(Object aResponse, File aFile)
        throws IOException
    {
        StringWriter out = new StringWriter();

        JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                .createJsonGenerator(out);

        jsonGenerator.writeObject(aResponse);
        FileUtils.writeStringToFile(aFile, out.toString());
    }

    /**
     * Set annotation preferences of users for a given project such as window size, annotation
     * layers , ...that can be saved to a file system
     *
     * @param aPreference
     *            the {@link AnnotationPreference} instance
     * @param aUsername
     *            {@link The annotator/curator who has logged in to the system}
     */
    public static void setAnnotationPreference(AnnotationPreference aPreference, String aUsername,
            RepositoryService aRepositoryService, AnnotationService aAnnotationService,
            BratAnnotatorModel abAnnotatorModel, Mode aMode)
        throws BeansException, FileNotFoundException, IOException
    {
        BeanWrapper wrapper = new BeanWrapperImpl(aPreference);
        // get annotation preference from file system
        try {
            for (Entry<Object, Object> entry : aRepositoryService.loadUserSettings(aUsername,
                    abAnnotatorModel.getProject()).entrySet()) {
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
            abAnnotatorModel.setWindowSize(aPreference.getWindowSize());
            abAnnotatorModel.setScrollPage(aPreference.isScrollPage());
            abAnnotatorModel.setDisplayLemmaSelected(aPreference.isDisplayLemmaSelected());
            // Get tagset using the id, from the properties file
            abAnnotatorModel.getAnnotationLayers().clear();
            if (aPreference.getAnnotationLayers() != null) {
                for (Long id : aPreference.getAnnotationLayers()) {
                    abAnnotatorModel.getAnnotationLayers().add(aAnnotationService.getTagSet(id));
                }
            }
        }
        // no preference found
        catch (Exception e) {

            // disable corefernce annotation for correction/curation pages for 0.4.0 release
            List<TagSet> tagSets = aAnnotationService.listTagSets(abAnnotatorModel.getProject());
            List<TagSet> corefTagSets = new ArrayList<TagSet>();
            for (TagSet tagSet : tagSets) {
                if(tagSet.getType().getName().equals("coreference type")||
                        tagSet.getType().getName().equals("coreference") ){
                    corefTagSets.add(tagSet);
                }
            }

            if(aMode.equals(Mode.CORRECTION) || aMode.equals(Mode.CORRECTION)){
                tagSets.removeAll(corefTagSets);
            }
            abAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(tagSets));
            /*
             * abAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(aAnnotationService
             * .listTagSets(abAnnotatorModel.getProject())));
             */
        }
    }

    // The magic bytes for ZIP
    // see http://notepad2.blogspot.de/2012/07/java-detect-if-stream-or-file-is-zip.html
    private static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

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
    public static boolean isZipValidWebanno(File aZipFile)
        throws ZipException, IOException
    {

        boolean isZipValidWebanno = false;
        ZipFile zip = new ZipFile(aZipFile);
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            if (entry.toString().replace("/", "").startsWith(ApplicationUtils.EXPORTED_PROJECT)
                    && entry.toString().replace("/", "").endsWith(".json")) {
                isZipValidWebanno = true;
                break;
            }
        }
        return isZipValidWebanno;
    }

    /**
     * Stores, for every tokens, the start and end position offsets : used for multiple span
     * annotations
     *
     * @return map of tokens begin and end positions
     */
    public static Map<Integer, Integer> offsets(JCas aJcas)
    {
        Map<Integer, Integer> offsets = new HashMap<Integer, Integer>();
        for (Token token : select(aJcas, Token.class)) {
            offsets.put(token.getBegin(), token.getEnd());
        }
        return offsets;
    }

    /**
     * For multiple span, get the start and end offsets
     */
    public static int[] getTokenStart(Map<Integer, Integer> aOffset, int aStart, int aEnd)
    {
        Iterator<Integer> it = aOffset.keySet().iterator();
        boolean startFound = false;
        boolean endFound = false;
        while (it.hasNext()) {
            int tokenStart = it.next();
            if (aStart >= tokenStart && aStart <= aOffset.get(tokenStart)) {
                aStart = tokenStart;
                startFound = true;
                if (endFound) {
                    break;
                }
            }
            if (aEnd >= tokenStart && aEnd <= aOffset.get(tokenStart)) {
                aEnd = aOffset.get(tokenStart);
                endFound = true;
                if (startFound) {
                    break;
                }
            }
        }
        return new int[] { aStart, aEnd };
    }

    /**
     * If the annotation type is limited to only a single token, but brat sends multiple tokens,
     * split them up
     *
     * @return Map of start and end offsets for the multiple token span
     */

    public static Map<Integer, Integer> getSplitedTokens(Map<Integer, Integer> aOffset, int aStart,
            int aEnd)
    {
        Map<Integer, Integer> startAndEndOfSplitedTokens = new HashMap<Integer, Integer>();
        Iterator<Integer> it = aOffset.keySet().iterator();
        while (it.hasNext()) {
            int tokenStart = it.next();
            if (aStart <= tokenStart && tokenStart <= aEnd) {
                startAndEndOfSplitedTokens.put(tokenStart, aOffset.get(tokenStart));
            }
        }
        return startAndEndOfSplitedTokens;
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

    public static void createTagset(Project aProjecct,
            de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet importedTagSet,
            RepositoryService aRepository, AnnotationService aAnnotationService)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        AnnotationType type = null;
        if (!aAnnotationService.existsType(importedTagSet.getTypeName(), importedTagSet.getType())) {
            type = new AnnotationType();
            type.setDescription(importedTagSet.getTypeDescription());
            type.setName(importedTagSet.getTypeName());
            type.setType(importedTagSet.getType());
            aAnnotationService.createType(type);
        }
        else {
            type = aAnnotationService.getType(importedTagSet.getTypeName(),
                    importedTagSet.getType());
        }

        if (importedTagSet != null) {

            de.tudarmstadt.ukp.clarin.webanno.model.TagSet newTagSet = new de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
            newTagSet.setDescription(importedTagSet.getDescription());
            newTagSet.setName(importedTagSet.getName());
            newTagSet.setLanguage(importedTagSet.getLanguage());
            newTagSet.setProject(aProjecct);
            newTagSet.setType(type);
            aAnnotationService.createTagSet(newTagSet, user);
            for (de.tudarmstadt.ukp.clarin.webanno.export.model.Tag tag : importedTagSet.getTags()) {
                Tag newTag = new Tag();
                newTag.setDescription(tag.getDescription());
                newTag.setName(tag.getName());
                newTag.setTagSet(newTagSet);
                aAnnotationService.createTag(newTag, user);
            }
        }
    }

    public static Project createProject(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aProject,
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
        project.setReverseDependencyDirection(aProject.isReverse());
        project.setMode(aProject.getMode());
        aRepository.createProject(project, user);
        return project;
    }

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

    public static void createSourceDocument(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
            Project aImportedProject, RepositoryService aRepository)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        for (SourceDocument importedSourceDocument : aImportedProjectSetting.getSourceDocuments()) {
            de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = new de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument();
            sourceDocument.setFormat(importedSourceDocument.getFormat());
            sourceDocument.setName(importedSourceDocument.getName());
            sourceDocument.setState(importedSourceDocument.getState());
            sourceDocument.setProject(aImportedProject);
            aRepository.createSourceDocument(sourceDocument, user);
        }
    }

    public static void createAnnotationDocument(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
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
            annotationDocument.setDocument(aRepository.getSourceDocument(
                    aImportedProject, importedAnnotationDocument.getName()));
            aRepository.createAnnotationDocument(annotationDocument);
        }
    }

    public static void createProjectPermission(
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project aImportedProjectSetting,
            Project aImportedProject, RepositoryService aRepository)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        for (de.tudarmstadt.ukp.clarin.webanno.export.model.ProjectPermission importedPermission : aImportedProjectSetting
                .getProjectPermissions()) {
            de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission = new de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission();
            permission.setLevel(importedPermission.getLevel());
            permission.setProject(aImportedProject);
            permission.setUser(importedPermission.getUser());
            aRepository.createProjectPermission(permission);
        }
    }

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
                File sourceFilePath = aRepository.exportSourceDocument(sourceDocument, aProject);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), sourceFilePath);
            }
        }
    }

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
                File annotationFilePath = aRepository.exportAnnotationDocument(sourceDocument,
                        aProject, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
            }
        }
    }

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
                File annotationFilePath = aRepository.exportAnnotationDocument(sourceDocument,
                        aProject, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
            }
        }
    }

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
     *
     * @param aSourceDocument
     * @param aUser
     * @return
     */
    public static boolean existFinishedDocument(de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument aSourceDocument, User aUser, RepositoryService aRepository, Project aProject)
    {
        List<de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument> annotationDocuments = aRepository.listAnnotationDocuments(
                aProject, aSourceDocument);
        boolean finishedAnnotationDocumentExist = false;
        for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : annotationDocuments) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocumentExist = true;
                break;
            }
        }
        return finishedAnnotationDocumentExist;

    }
}
