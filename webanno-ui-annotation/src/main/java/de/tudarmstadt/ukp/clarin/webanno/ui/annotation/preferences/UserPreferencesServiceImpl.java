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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.preferences;

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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ColoringStrategyType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class UserPreferencesServiceImpl
    implements UserPreferencesService
{
    private final BratProperties defaultPreferences;
    private final ProjectService repositoryService;
    private final AnnotationSchemaService annotationService;
    
    public UserPreferencesServiceImpl(BratProperties aDefaultPreferences,
            ProjectService aRepositoryService, AnnotationSchemaService aAnnotationService)
    {
        defaultPreferences = aDefaultPreferences;
        repositoryService = aRepositoryService;
        annotationService = aAnnotationService;
    }

    @Override
    public AnnotationPreference loadPreferences(Project aProject, String aUsername, Mode aMode)
        throws IOException
    {
        return loadLegacyPreferences(aProject, aUsername, aMode);
    }
    
    @Override
    public void savePreferences(Project aProject, String aUsername, Mode aMode,
            AnnotationPreference aPref)
        throws IOException
    {
        // TODO Auto-generated method stub
        
    }

    public void saveLegacyPreferences(Project aProject, String aUsername, Mode aMode,
            AnnotationPreference aPref)
        throws IOException
    {
        // TODO Auto-generated method stub
    }

    private AnnotationPreference loadLegacyPreferences(Project aProject, String aUsername,
            Mode aMode)
    {
        AnnotationPreference preference = new AnnotationPreference();
        
        BeanWrapper wrapper = new BeanWrapperImpl(preference);
        
        // get annotation preference from file system
        try {
            Properties props = repositoryService.loadUserSettings(aUsername, aProject);
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
}
