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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.page;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class contains Utility methods that can be used in Project settings
 */
public class PreferencesUtil
{
    /**
     * Set annotation preferences of users for a given project such as window size, annotation
     * layers,... reading from the file system.
     * @param aAnnotationService the annotation service.
     * @param aState
     *            The {@link AnnotatorState} that will be populated with preferences from the
     *            file
     * @param aUsername
     *            The {@link User} for whom we need to read the preference (preferences are stored
     *            per user)
     *
     * @throws BeansException hum?
     * @throws IOException hum?
     */
    public static void loadPreferences(UserPreferencesService aPrefService,
            AnnotationSchemaService aAnnotationService, AnnotatorState aState, String aUsername)
        throws BeansException, IOException
    {
        AnnotationPreference preference = aPrefService.loadPreferences(aState.getProject(),
                aUsername, aState.getMode());

        aState.setPreferences(preference);
        
        // set layers according to preferences
        aState.setAnnotationLayers(aAnnotationService
                .listAnnotationLayer(aState.getProject()).stream()
                // Token layer cannot be selected!
                .filter(l -> !Token.class.getName().equals(l.getName()))
                // Only allow enabled layers
                .filter(l -> l.isEnabled()) 
                .filter(l -> !preference.getHiddenAnnotationLayerIds().contains(l.getId()))
                .collect(Collectors.toList()));
        
        // set default layer according to preferences
        Optional<AnnotationLayer> defaultLayer = aState.getAnnotationLayers().stream()
                .filter(layer -> Objects.equals(layer.getId(), preference.getDefaultLayer()))
                .findFirst();
        if (defaultLayer.isPresent()) {
            aState.setDefaultAnnotationLayer(defaultLayer.get());
            aState.setSelectedAnnotationLayer(defaultLayer.get());
        }
    }

    public static void savePreference(UserPreferencesService aPrefService, AnnotatorState aState,
            String aUsername)
        throws IOException
    {
        aPrefService.savePreferences(aState.getProject(), aUsername, aState.getMode(),
                aState.getPreferences());
    }
}
