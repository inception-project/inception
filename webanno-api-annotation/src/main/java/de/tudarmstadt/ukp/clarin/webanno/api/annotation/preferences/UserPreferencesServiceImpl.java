/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SETTINGS_FOLDER;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ColoringStrategyType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class UserPreferencesServiceImpl
    implements UserPreferencesService
{
    /**
     * The annotation preference properties file name.
     */
    private static final String ANNOTATION_PREFERENCE_PROPERTIES_FILE = "annotation.properties";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final BratProperties defaultPreferences;
    private final AnnotationSchemaService annotationService;
    
    @Value(value = "${repository.path}")
    private File dir;
    
    public UserPreferencesServiceImpl(BratProperties aDefaultPreferences,
            AnnotationSchemaService aAnnotationService)
    {
        defaultPreferences = aDefaultPreferences;
        annotationService = aAnnotationService;
    }

    @Override
    public synchronized AnnotationPreference loadPreferences(Project aProject, String aUsername,
            Mode aMode)
        throws IOException
    {
        // TODO Use modular preference loading once it is available and if there is a corresponding
        // data file. Otherwise, fall back to loading the legacy preferences
        
        return loadLegacyPreferences(aProject, aUsername, aMode);
    }

    @Override
    public synchronized void savePreferences(Project aProject, String aUsername, Mode aMode,
            AnnotationPreference aPref)
        throws IOException
    {
        // TODO Switch to a new and modular way of writing preferences
        
        saveLegacyPreferences(aProject, aUsername, aMode, aPref);
    }

    /**
     * Save annotation references, such as {@code BratAnnotator#windowSize}..., in a properties file
     * so that they are not required to configure every time they open the document.
     *
     * @param aUsername
     *            the user name
     * @param aMode
     *            differentiate the setting, either it is for {@code AnnotationPage} or
     *            {@code CurationPage}
     * @param aPref
     *            The Object to be saved as preference in the properties file.
     * @param aProject
     *            The project where the user is working on.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private void saveLegacyPreferences(Project aProject, String aUsername, Mode aMode,
            AnnotationPreference aPref)
        throws IOException
    {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(aPref);
        Properties props = new Properties();
        for (PropertyDescriptor value : wrapper.getPropertyDescriptors()) {
            if (wrapper.getPropertyValue(value.getName()) == null) {
                continue;
            }
            props.setProperty(aMode + "." + value.getName(),
                    wrapper.getPropertyValue(value.getName()).toString());
        }
        String propertiesPath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aProject.getId() + "/" + SETTINGS_FOLDER + "/" + aUsername;
        // append existing preferences for the other mode
        if (new File(propertiesPath, ANNOTATION_PREFERENCE_PROPERTIES_FILE).exists()) {
            Properties properties = loadLegacyPreferencesFile(aUsername, aProject);
            for (Entry<Object, Object> entry : properties.entrySet()) {
                String key = entry.getKey().toString();
                // Maintain other Modes of annotations confs than this one
                if (!key.substring(0, key.indexOf(".")).equals(aMode.toString())) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // for (String name : props.stringPropertyNames()) {
        // log.info("{} = {}", name, props.getProperty(name));
        // }

        FileUtils.forceMkdir(new File(propertiesPath));
        props.store(new FileOutputStream(
                new File(propertiesPath, ANNOTATION_PREFERENCE_PROPERTIES_FILE)), null);

//        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
//                String.valueOf(aProject.getId()))) {
//            log.info("Saved preferences for user [{}] in project [{}]({})", aUsername,
//                    aProject.getName(), aProject.getId());
//        }
    }

    private AnnotationPreference loadLegacyPreferences(Project aProject, String aUsername,
            Mode aMode)
    {
        AnnotationPreference preference = new AnnotationPreference();
        
        BeanWrapper wrapper = new BeanWrapperImpl(preference);
        
        // get annotation preference from file system
        try {
            Properties props = loadLegacyPreferencesFile(aUsername, aProject);
            for (Entry<Object, Object> entry : props.entrySet()) {
                String property = entry.getKey().toString();
                int index = property.indexOf(".");
                String propertyName = property.substring(index + 1);
                String mode = property.substring(0, index);
                if (wrapper.isWritableProperty(propertyName) && mode.equals(aMode.getName())) {
                    if (AnnotationPreference.class.getDeclaredField(propertyName)
                            .getGenericType() instanceof ParameterizedType) {
                        if (entry.getValue().toString().startsWith("[")) { // its a list
                            List<String> value = Arrays.asList(
                                    StringUtils.replaceChars(entry.getValue().toString(), "[]", "").split(","));
                            if (!value.get(0).equals("")) {
                                wrapper.setPropertyValue(propertyName, value);
                            }
                        }
                        else if (entry.getValue().toString().startsWith("{")) { // its a map
                            String s = StringUtils.replaceChars(entry.getValue().toString(), "{}",
                                    "");
                            Map<String, String> value = Arrays.stream(s.split(","))
                                    .map(x -> x.split("="))
                                    .collect(Collectors.toMap(x -> x[0], x -> x[1]));
                            wrapper.setPropertyValue(propertyName, value);
                        }
                    }
                    else {
                        wrapper.setPropertyValue(propertyName, entry.getValue());
                    }
                }
            }
        }
        // no preference found
        catch (Exception e) {
            preference.setHiddenAnnotationLayerIds(new HashSet<>());
            preference.setWindowSize(defaultPreferences.getPageSize());
            preference.setScrollPage(defaultPreferences.isAutoScroll());
            preference.setRememberLayer(defaultPreferences.isRememberLayer());
        }
        
        // Get color preferences for each layer, init with default if not found
        Map<Long, ColoringStrategyType> colorPerLayer = preference.getColorPerLayer();
        if (colorPerLayer == null) {
            colorPerLayer = new HashMap<>();
            preference.setColorPerLayer(colorPerLayer);
        }
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!colorPerLayer.containsKey(layer.getId())) {
                colorPerLayer.put(layer.getId(), ColoringStrategy
                        .getBestInitialStrategy(annotationService, layer, preference));
            }
        }
        
        return preference;
    }
    
    /**
     * Load annotation preferences from a property file.
     *
     * @param aUsername
     *            the user name.
     * @param aProject
     *            the project where the user is working on.
     * @return the properties.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private Properties loadLegacyPreferencesFile(String aUsername, Project aProject)
        throws IOException
    {
        Properties property = new Properties();
        property.load(new FileInputStream(new File(dir.getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + SETTINGS_FOLDER + "/" + aUsername + "/"
                + ANNOTATION_PREFERENCE_PROPERTIES_FILE)));
        return property;
    }
}
