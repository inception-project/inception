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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SettingsService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * This class contains Utility methods that can be used in Project settings
 */
public class PreferencesUtil
{
    public static final String META_INF = "META-INF";
    public static final String SOURCE = "source";
    public static final String ANNOTATION_AS_SERIALISED_CAS = "annotation_ser";
    public static final String CURATION_AS_SERIALISED_CAS = "curation_ser";
    public static final String GUIDELINE = "guideline";
    public static final String LOG_DIR = "log";
    public static final String EXPORTED_PROJECT = "exportedproject";

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
     *            The {@link AnnotatorState} that will be populated with preferences from the
     *            file
     * @param aMode the mode.
     * @throws BeansException hum?
     * @throws IOException hum?
     */
    public static void loadPreferences(String aUsername, SettingsService aSettingsService,
            ProjectService aRepositoryService, AnnotationSchemaService aAnnotationService,
            AnnotatorState aBModel, Mode aMode)
        throws BeansException, IOException
    {
        AnnotationPreference preference = new AnnotationPreference();
        
        BeanWrapper wrapper = new BeanWrapperImpl(preference);
        
        // get annotation preference from file system
        try {
            Properties props = aRepositoryService.loadUserSettings(aUsername, aBModel.getProject());
            for (Entry<Object, Object> entry : props.entrySet()) {
                String property = entry.getKey().toString();
                int index = property.lastIndexOf(".");
                String propertyName = property.substring(index + 1);
                String mode = property.substring(0, index);
                if (wrapper.isWritableProperty(propertyName) && mode.equals(aMode.getName())) {
                    if (AnnotationPreference.class.getDeclaredField(propertyName)
                            .getGenericType() instanceof ParameterizedType) {
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

            // Get tagset using the id, from the properties file
            aBModel.getAnnotationLayers().clear();
            if (preference.getAnnotationLayers() != null) {
                for (Long id : preference.getAnnotationLayers()) {
                    aBModel.getAnnotationLayers().add(aAnnotationService.getLayer(id));
                }
            }
            else {
                // If no layer preferences are defined, then just assume all layers are enabled
                List<AnnotationLayer> layers = aAnnotationService.listAnnotationLayer(aBModel
                        .getProject());
                aBModel.setAnnotationLayers(layers);
            }
        }
        // no preference found
        catch (Exception e) {
            // If no layer preferences are defined, then just assume all layers are enabled
            List<AnnotationLayer> layers = aAnnotationService.listAnnotationLayer(aBModel
                    .getProject());
            aBModel.setAnnotationLayers(layers);
            preference.setWindowSize(aSettingsService.getNumberOfSentences());
        }
        
        aBModel.setPreferences(preference);
    }

    public static void savePreference(AnnotatorState aBModel, ProjectService aRepository)
        throws FileNotFoundException, IOException
    {
        AnnotationPreference preference = aBModel.getPreferences();
        ArrayList<Long> layers = new ArrayList<>();

        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            layers.add(layer.getId());
        }
        preference.setAnnotationLayers(layers);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        aRepository.saveUserSettings(username, aBModel.getProject(), aBModel.getMode(), preference);
    }
}
