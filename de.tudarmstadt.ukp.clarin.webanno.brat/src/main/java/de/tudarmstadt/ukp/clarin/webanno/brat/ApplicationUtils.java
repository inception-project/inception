/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.persistence.NoResultException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
/**
 * This class Utility methods that can be used application wide
 * @author Seid Muhie Yimam
 *
 */
public class ApplicationUtils
{

    private static MappingJacksonHttpMessageConverter jsonConverter;

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
            if (authority.getRole().equals("ROLE_ADMIN")) {
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
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean projectAdmin = false;
        if (!roleAdmin) {

            try {
                List<ProjectPermission> permissionLevels = aProjectRepository.listProjectPermisionLevel(aUser, aProject);
                    for(ProjectPermission permissionLevel:permissionLevels) {
                        if(StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
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
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean curator = false;
        if (!roleAdmin) {

            try {
                List<ProjectPermission> permissionLevels = aProjectRepository.listProjectPermisionLevel(aUser, aProject);
                    for(ProjectPermission permissionLevel:permissionLevels) {
                        if(StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
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
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean user = false;
        if (!roleAdmin) {

            try {
                List<ProjectPermission> permissionLevels = aProjectRepository.listProjectPermisionLevel(aUser, aProject);
                    for(ProjectPermission permissionLevel:permissionLevels) {
                        if(StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
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
     * Set annotation preferences of users for a given project such as window size, annotation layers
     *  , ...that can be saved to a file system
     * @param aPreference the {@link AnnotationPreference} instance
     * @param aUsername {@link The annotator/curator who has logged in to the system}
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
                if (wrapper.isWritableProperty(propertyName) && mode.equals(aMode.name())) {

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
            abAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(aAnnotationService
                    .listTagSets(abAnnotatorModel.getProject())));
        }
    }

    // The magic bytes for ZIP
    // see http://notepad2.blogspot.de/2012/07/java-detect-if-stream-or-file-is-zip.html
    private static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };
    public static  boolean isZipStream(InputStream in) {
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
        } catch (IOException e) {
         isZip = false;
        }
        return isZip;
       }
}
