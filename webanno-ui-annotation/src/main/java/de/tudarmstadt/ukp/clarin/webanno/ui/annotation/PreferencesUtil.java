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

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.preferences.UserPreferencesService;

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
     * @param aAnnotationService the annotation service.
     * @param aState
     *            The {@link AnnotatorState} that will be populated with preferences from the
     *            file
     * @throws BeansException hum?
     * @throws IOException hum?
     */
    public static void loadPreferences(String aUsername, AnnotationSchemaService aAnnotationService,
            UserPreferencesService aPrefService, AnnotatorState aState)
        throws BeansException, IOException
    {
        AnnotationPreference preference = aPrefService.loadPreferences(aState.getProject(),
                aUsername, aState.getMode());

        aState.setPreferences(preference);
        
        // set layers according to preferences
        aState.setAnnotationLayers(aAnnotationService
                .listAnnotationLayer(aState.getProject()).stream()
                .filter(l -> l.isEnabled())// only allow enabled layers
                .filter(l -> !preference.getHiddenAnnotationLayerIds().contains(l.getId()))
                .collect(Collectors.toList()));
    }

    public static void savePreference(AnnotatorState aBModel, ProjectService aRepository)
        throws IOException
    {
        AnnotationPreference preference = aBModel.getPreferences();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        aRepository.saveUserSettings(username, aBModel.getProject(), aBModel.getMode(), preference);
    }
}
