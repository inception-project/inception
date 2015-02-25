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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.export.CrowdJob;
import de.tudarmstadt.ukp.clarin.webanno.model.export.MiraTemplate;

/**
 * This class contains Utility methods that can be used in Project settings
 *
 * @author Seid Muhie Yimam
 *
 */
public class ProjectUtil
{

    private static MappingJacksonHttpMessageConverter jsonConverter;

    public static final String META_INF = "META-INF";
    public static final String SOURCE = "source";
    public static final String ANNOTATION_AS_SERIALISED_CAS = "annotation_ser";
    public static final String CURATION_AS_SERIALISED_CAS = "curation_ser";
    public static final String GUIDELINE = "guideline";
    public static final String LOG_DIR = "log";
    public static final String EXPORTED_PROJECT = "exportedproject";

    public static void setJsonConverter(MappingJacksonHttpMessageConverter aJsonConverter)
    {
        jsonConverter = aJsonConverter;
    }

    private static final Log LOG = LogFactory.getLog(ProjectUtil.class);

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     * 
     * @param aLineSeparatedTags the line.
     * @return the parsed line.
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
     * 
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a global admin.
     */
    public static boolean isSuperAdmin(RepositoryService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.listAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getAuthority().equals(Role.ROLE_ADMIN.name())) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * IS project creator
     * 
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a project creator
     */
    public static boolean isProjectCreator(RepositoryService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.listAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getAuthority().equals(Role.ROLE_PROJECT_CREATOR.name())) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * Determine if the User is allowed to update a project
     *
     * @param aProject the project
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user may update a project.
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
     * @param aProject the project.
     * @param aProjectRepository the respository service.
     * @param aUser the user.
     * @return if the user is a curator.
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
     * @param aProject the project.
     * @param aProjectRepository the respository service.
     * @param aUser the user.
     * @return if the user is a member.
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
     * @param aObject the object.
     * @param aFile the file
     * @throws IOException if an I/O error occurs.
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
     * @param aRepositoryService the repository service.
     * @param aAnnotationService the annotation service.
     * @param aBModel
     *            The {@link BratAnnotatorModel} that will be populated with preferences from the
     *            file
     * @param aMode the mode.
     * @throws BeansException hum?
     * @throws IOException hum?
     */
    public static void setAnnotationPreference(String aUsername,
            RepositoryService aRepositoryService, AnnotationService aAnnotationService,
            BratAnnotatorModel aBModel, Mode aMode)
        throws BeansException, IOException
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
                    aBModel.getAnnotationLayers().add(aAnnotationService.getLayer(id));
                }
            }
        }
        // no preference found
        catch (Exception e) {

            /*
             * // disable corefernce annotation for correction/curation pages for 0.4.0 release
             * List<TagSet> tagSets = aAnnotationService.listTagSets(aBModel.getProject());
             * List<TagSet> corefTagSets = new ArrayList<TagSet>(); List<TagSet> noFeatureTagSet =
             * new ArrayList<TagSet>(); for (TagSet tagSet : tagSets) { if (tagSet.getLayer() ==
             * null || tagSet.getFeature() == null) { noFeatureTagSet.add(tagSet); } else if
             * (tagSet.getLayer().getType().equals(ChainAdapter.CHAIN)) { corefTagSets.add(tagSet);
             * } }
             *
             * if (aMode.equals(Mode.CORRECTION) || aMode.equals(Mode.AUTOMATION) ||
             * aMode.equals(Mode.CURATION)) { tagSets.removeAll(corefTagSets); }
             * tagSets.remove(noFeatureTagSet); aBModel.setAnnotationLayers(new
             * HashSet<TagSet>(tagSets));
             */
            /*
             * abAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(aAnnotationService
             * .listTagSets(abAnnotatorModel.getProject())));
             */

            List<AnnotationLayer> layers = aAnnotationService.listAnnotationLayer(aBModel
                    .getProject());
            aBModel.setAnnotationLayers(layers);
        }
    }

    // The magic bytes for ZIP
    // see
    // http://notepad2.blogspot.de/2012/07/java-detect-if-stream-or-file-is-zip.html
    private static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

    /**
     * check if the {@link InputStream} provided is a zip file
     * 
     * @param in the stream.
     * @return if it is a ZIP file.
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
     * @param aZipFile the file.
     * @return if it is valid.
     * @throws ZipException if the ZIP file is corrupt.
     * @throws IOException if an I/O error occurs.
     *
     */
    @SuppressWarnings({ "rawtypes" })
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
     * 
     * @param aName a name.
     * @return if the name is valid.
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
     * @param aProjecct a project. 
     * @param aImportedProjectSetting the settings. 
     * @param aRepository the repository service.
     * @param aAnnotationService the annotation service.
     * @return hum?
     * @throws IOException if an I/O error occurs.
     */
    public static Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> createLayer(
            Project aProjecct,
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            RepositoryService aRepository, AnnotationService aAnnotationService)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        List<de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet> importedTagSet = aImportedProjectSetting
                .getTagSets();
        if (aImportedProjectSetting.getVersion() == 0) {// this is projects prio
                                                        // // to version 2.0
            createV0TagSet(aProjecct, importedTagSet, aAnnotationService, user);
            return new HashMap<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature>();
        }
        return createV1Layer(aProjecct, aImportedProjectSetting, aAnnotationService, user);

    }

    private static void createV0TagSet(Project aProjecct,
            List<de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet> importedTagSet,
            AnnotationService aAnnotationService, User user)
        throws IOException
    {
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

    private static Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> createV1Layer(
            Project aProject,
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            AnnotationService aAnnotationService, User aUser)
        throws IOException
    {
        Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> featuresMap = new HashMap<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature>();
        Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer, AnnotationLayer> layersMap = new HashMap<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer, AnnotationLayer>();
        for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exLayer : aImportedProjectSetting
                .getLayers()) {
            if (aAnnotationService.existsLayer(exLayer.getName(), exLayer.getType(), aProject)) {
                AnnotationLayer layer = aAnnotationService.getLayer(exLayer.getName(), aProject);
                setLayer(aAnnotationService, layer, exLayer, aProject, aUser);
                layersMap.put(exLayer, layer);
                for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature exfeature : exLayer
                        .getFeatures()) {
                    if (aAnnotationService.existsFeature(exfeature.getName(), layer)) {
                        AnnotationFeature feature = aAnnotationService.getFeature(
                                exfeature.getName(), layer);
                        setFeature(aAnnotationService, feature, exfeature, aProject, aUser);
                        featuresMap.put(exfeature, feature);
                        continue;
                    }
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    setFeature(aAnnotationService, feature, exfeature, aProject, aUser);
                    featuresMap.put(exfeature, feature);
                }
            }
            else {
                AnnotationLayer layer = new AnnotationLayer();
                setLayer(aAnnotationService, layer, exLayer, aProject, aUser);
                layersMap.put(exLayer, layer);
                for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature exfeature : exLayer
                        .getFeatures()) {
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    setFeature(aAnnotationService, feature, exfeature, aProject, aUser);
                    featuresMap.put(exfeature, feature);
                }
            }
        }

        for (de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet exTagSet : aImportedProjectSetting
                .getTagSets()) {
            TagSet tagSet = new TagSet();
            createTagSet(tagSet, exTagSet, aProject, aUser, aAnnotationService);
        }

        for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exLayer : aImportedProjectSetting
                .getLayers()) {
            if (exLayer.getAttachType() != null) {
                AnnotationLayer layer = aAnnotationService.getLayer(exLayer.getName(), aProject);
                AnnotationLayer attachLayer = aAnnotationService.getLayer(exLayer.getAttachType()
                        .getName(), aProject);
                layer.setAttachType(attachLayer);
                aAnnotationService.createLayer(layersMap.get(exLayer), aUser);
            }
            if (exLayer.getAttachFeature() != null) {
                layersMap.get(exLayer)
                        .setAttachFeature(featuresMap.get(exLayer.getAttachFeature()));
                aAnnotationService.createLayer(layersMap.get(exLayer), aUser);
            }

            for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature eXFeature : exLayer
                    .getFeatures()) {
                if (eXFeature.getTagSet() != null) {
                    featuresMap.get(eXFeature)
                            .setTagset(
                                    aAnnotationService.getTagSet(eXFeature.getTagSet().getName(),
                                            aProject));
                }
            }
        }
        return featuresMap;
    }

    public static void createTagSet(TagSet aTagSet,
            de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet aExTagSet, Project aProject,
            User aUser, AnnotationService aAnnotationService)
        throws IOException
    {
        aTagSet.setCreateTag(aExTagSet.isCreateTag());
        aTagSet.setDescription(aExTagSet.getDescription());
        aTagSet.setLanguage(aExTagSet.getLanguage());
        aTagSet.setName(aExTagSet.getName());
        aTagSet.setProject(aProject);
        aAnnotationService.createTagSet(aTagSet, aUser);

        for (de.tudarmstadt.ukp.clarin.webanno.model.export.Tag exTag : aExTagSet.getTags()) {
            // du not duplicate tag
            if (aAnnotationService.existsTag(exTag.getName(), aTagSet)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setDescription(exTag.getDescription());
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            aAnnotationService.createTag(tag, aUser);
        }
    }

    public static void setLayer(AnnotationService aAnnotationService, AnnotationLayer aLayer,
            de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer aExLayer,
            Project aProject, User aUser)
        throws IOException
    {
        aLayer.setAllowStacking(aExLayer.isAllowStacking());
        aLayer.setBuiltIn(aExLayer.isBuiltIn());
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
        aAnnotationService.createLayer(aLayer, aUser);
    }

    public static void setFeature(AnnotationService aAnnotationService, AnnotationFeature aFeature,
            de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature aExFeature,
            Project aProject, User aUser)
    {
        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setLayer(aFeature.getLayer());
        aFeature.setType(aExFeature.getType());
        aFeature.setName(aExFeature.getName());
        aAnnotationService.createFeature(aFeature);
    }

    /**
     * create new {@link Project} from the
     * {@link de.tudarmstadt.ukp.clarin.webanno.model.export.Project} model
     * @param aProject the project
     * @param aRepository the repository service.
     * @return the project.
     * @throws IOException if an I/O error occurs.
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
     * 
     * @param aImportedProjectSetting  the exported project.
     * @param aImportedProject the project.
     * @param aRepository the repository service.
     * @param aFeatureMap hum?
     * @throws IOException if an I/O error occurs.
     */
    public static void createSourceDocument(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            Project aImportedProject,
            RepositoryService aRepository,
            Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> aFeatureMap)
        throws IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        for (de.tudarmstadt.ukp.clarin.webanno.model.export.SourceDocument importedSourceDocument : aImportedProjectSetting
                .getSourceDocuments()) {
            SourceDocument sourceDocument = new SourceDocument();
            sourceDocument.setFormat(importedSourceDocument.getFormat());
            sourceDocument.setName(importedSourceDocument.getName());
            sourceDocument.setState(importedSourceDocument.getState());
            sourceDocument.setProject(aImportedProject);
            sourceDocument.setTimestamp(importedSourceDocument.getTimestamp());
            if (aFeatureMap.size() > 0) {
                sourceDocument.setFeature(aFeatureMap.get(importedSourceDocument.getFeature()));
            }
            sourceDocument.setProcessed(false);// automation re-start in the new
                                               // project settings
            sourceDocument.setTrainingDocument(importedSourceDocument.isTrainingDocument());
            sourceDocument.setSentenceAccessed(importedSourceDocument.getSentenceAccessed());
            aRepository.createSourceDocument(sourceDocument, user);
        }
    }

    public static void createMiraTemplate(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            RepositoryService aRepository,
            Map<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature, AnnotationFeature> aFeatureMaps)
    {
        for (MiraTemplate exTemplate : aImportedProjectSetting.getMiraTemplates()) {
            de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate template = new de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate();
            template.setAnnotateAndPredict(exTemplate.isAnnotateAndPredict());
            template.setAutomationStarted(false);
            template.setCurrentLayer(exTemplate.isCurrentLayer());
            template.setResult("---");
            template.setTrainFeature(aFeatureMaps.get(exTemplate.getTrainFeature()));
            Set<AnnotationFeature> otherFeatures = new HashSet<AnnotationFeature>();
            if (exTemplate.getOtherFeatures() != null) {
                for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature exOtherFeature : exTemplate
                        .getOtherFeatures()) {
                    otherFeatures.add(aFeatureMaps.get(exOtherFeature));
                }
                template.setOtherFeatures(otherFeatures);
            }
            aRepository.createTemplate(template);
        }
    }

    public static void createCrowdJob(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            RepositoryService aRepository, Project aImportedProject)
        throws IOException
    {
        for (CrowdJob exCrowdJob : aImportedProjectSetting.getCrowdJobs()) {
            de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob crowdJob = new de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob();
            crowdJob.setApiKey(exCrowdJob.getApiKey());
            crowdJob.setLink(exCrowdJob.getLink());
            crowdJob.setName(exCrowdJob.getName());
            crowdJob.setProject(aImportedProject);
            crowdJob.setStatus(exCrowdJob.getStatus());
            crowdJob.setTask1Id(exCrowdJob.getTask1Id());
            crowdJob.setTask2Id(exCrowdJob.getTask2Id());
            crowdJob.setUseGoldSents(exCrowdJob.getUseGoldSents());
            crowdJob.setUseSents(exCrowdJob.getUseSents());

            Set<SourceDocument> documents = new HashSet<SourceDocument>();

            for (de.tudarmstadt.ukp.clarin.webanno.model.export.SourceDocument exDocument : exCrowdJob
                    .getDocuments()) {
                documents
                        .add(aRepository.getSourceDocument(aImportedProject, exDocument.getName()));
            }
            crowdJob.setDocuments(documents);

            Set<SourceDocument> goldDocuments = new HashSet<SourceDocument>();

            for (de.tudarmstadt.ukp.clarin.webanno.model.export.SourceDocument exDocument : exCrowdJob
                    .getGoldDocuments()) {
                goldDocuments.add(aRepository.getSourceDocument(aImportedProject,
                        exDocument.getName()));
            }
            crowdJob.setGoldDocuments(goldDocuments);

            aRepository.createCrowdJob(crowdJob);
        }
    }

    /**
     * Create {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument} from exported
     * {@link AnnotationDocument}
     * 
     * @param aImportedProjectSetting the imported project.
     * @param aImportedProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
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
            annotationDocument
                    .setSentenceAccessed(importedAnnotationDocument.getSentenceAccessed());
            aRepository.createAnnotationDocument(annotationDocument);
        }
    }

    /**
     * Create {@link ProjectPermission} from the exported
     * {@link de.tudarmstadt.ukp.clarin.webanno.model.export.ProjectPermission}
     * @param aImportedProjectSetting the imported project.
     * @param aImportedProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    public static void createProjectPermission(
            de.tudarmstadt.ukp.clarin.webanno.model.export.Project aImportedProjectSetting,
            Project aImportedProject, RepositoryService aRepository, boolean aGenerateUsers,
            UserDao aUserDao)
        throws IOException
    {
        Set<String> users = new HashSet<>();
        
        for (de.tudarmstadt.ukp.clarin.webanno.model.export.ProjectPermission importedPermission : aImportedProjectSetting
                .getProjectPermissions()) {
            ProjectPermission permission = new ProjectPermission();
            permission.setLevel(importedPermission.getLevel());
            permission.setProject(aImportedProject);
            permission.setUser(importedPermission.getUser());
            aRepository.createProjectPermission(permission);
            
            users.add(importedPermission.getUser());
        }
        
        if (aGenerateUsers) {
            for (String user : users) {
                if (!aRepository.existsUser(user)) {
                    User u = new User();
                    u.setUsername(user);
                    u.setEnabled(false);
                    aUserDao.create(u);
                }
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
    @SuppressWarnings("rawtypes")
    public static void createSourceDocumentContent(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(SOURCE)) {
                String fileName = FilenameUtils.getName(entryName);
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File sourceFilePath = aRepository.exportSourceDocument(sourceDocument);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), sourceFilePath);
                
                LOG.info("Imported source document content for source document ["
                        + sourceDocument.getId() + "] in project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
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
    @SuppressWarnings("rawtypes")
    public static void createAnnotationDocumentContent(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(ANNOTATION_AS_SERIALISED_CAS+"/")) {
                String fileName = entryName.replace(ANNOTATION_AS_SERIALISED_CAS+"/", "");

                // the user annotated the document is file name minus extension
                // (anno1.ser)
                String username = FilenameUtils.getBaseName(fileName).replace(".ser", "");

                // name of the annotation document
                fileName = fileName.replace(FilenameUtils.getName(fileName), "").replace("/", "");
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File annotationFilePath = aRepository.exportserializedCas(sourceDocument, username);

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
    @SuppressWarnings("rawtypes")
    public static void createCurationDocumentContent(ZipFile zip, Project aProject,
            RepositoryService aRepository)
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
                de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument = aRepository
                        .getSourceDocument(aProject, fileName);
                File annotationFilePath = aRepository.exportserializedCas(sourceDocument, username);

                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), annotationFilePath);
                
                LOG.info("Imported curation document content for user [" + username
                        + "] for source document [" + sourceDocument.getId() + "] in project ["
                        + aProject.getName() + "] with id [" + aProject.getId() + "]");
            }
        }
    }

    /**
     * copy guidelines from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static void createProjectGuideline(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(GUIDELINE)) {
                String filename = FilenameUtils.getName(entry.getName());
                File guidelineDir = aRepository.exportGuidelines(aProject);
                FileUtils.forceMkdir(guidelineDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(guidelineDir,
                        filename));
                
                LOG.info("Imported guideline [" + filename + "] for project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            }
        }
    }

    /**
     * copy Project META_INF from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static void createProjectMetaInf(ZipFile zip, Project aProject,
            RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);

            if (entryName.startsWith(META_INF)) {
                File metaInfDir = new File(aRepository.exportProjectMetaInf(aProject),
                        FilenameUtils.getPath(entry.getName().replace(META_INF, "")));
                // where the file reside in the META-INF/... directory
                FileUtils.forceMkdir(metaInfDir);
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry), new File(metaInfDir,
                        FilenameUtils.getName(entry.getName())));
                
                LOG.info("Imported META-INF for project [" + aProject.getName() + "] with id ["
                        + aProject.getId() + "]");
            }
        }
    }

    /**
     * copy project log files from the exported project
     * @param zip the ZIP file.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @throws IOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static void createProjectLog(ZipFile zip, Project aProject, RepositoryService aRepository)
        throws IOException
    {
        for (Enumeration zipEnumerate = zip.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = normalizeEntryName(entry);
            
            if (entryName.startsWith(LOG_DIR)) {
                FileUtils.copyInputStreamToFile(zip.getInputStream(entry),
                        aRepository.exportProjectLog(aProject));
                LOG.info("Imported log for project [" + aProject.getName() + "] with id ["
                        + aProject.getId() + "]");
            }
        }
    }

    /**
     * Return true if there exist at least one annotation document FINISHED for annotation for this
     * {@link SourceDocument}
     * @param aSourceDocument the source document.
     * @param aUser the user.
     * @param aProject the project.
     * @param aRepository the repository service.
     * @return if a finished document exists.
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

        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            layers.add(layer.getId());
        }
        preference.setAnnotationLayers(layers);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        aRepository.saveUserSettings(username, aBModel.getProject(), aBModel.getMode(), preference);
    }

    public static de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer, AnnotationService aAnnotationService)
    {
        de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exLayer = new de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer();
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setBuiltIn(aLayer.isBuiltIn());
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

        List<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature> exFeatures = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature>();
        for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aLayer)) {
            de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature exFeature = new de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature();
            exFeature.setDescription(feature.getDescription());
            exFeature.setEnabled(feature.isEnabled());
            exFeature.setName(feature.getName());
            exFeature.setProjectName(feature.getProject().getName());
            exFeature.setType(feature.getType());
            exFeature.setUiName(feature.getUiName());
            exFeature.setVisible(feature.isVisible());

            if (feature.getTagset() != null) {
                TagSet tagSet = feature.getTagset();
                de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet exTagSet = new de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet();
                exTagSet.setDescription(tagSet.getDescription());
                exTagSet.setLanguage(tagSet.getLanguage());
                exTagSet.setName(tagSet.getName());
                exTagSet.setCreateTag(tagSet.isCreateTag());

                List<de.tudarmstadt.ukp.clarin.webanno.model.export.Tag> exportedTags = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.Tag>();
                for (Tag tag : aAnnotationService.listTags(tagSet)) {
                    de.tudarmstadt.ukp.clarin.webanno.model.export.Tag exTag = new de.tudarmstadt.ukp.clarin.webanno.model.export.Tag();
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

    private static String normalizeEntryName(ZipEntry aEntry)
    {
        // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
        String entryName = aEntry.toString();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
       
        return entryName;
    }
}
