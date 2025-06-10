/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.SETTINGS_FOLDER;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs.KEY_ANCHORING_MODE;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationLayerVisibilityState.KEY_LAYERS_STATE;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPageLayoutState.KEY_LAYOUT_STATE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.replaceChars;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationAutoConfiguration#userPreferencesService}.
 * </p>
 */
public class UserPreferencesServiceImpl
    implements UserPreferencesService
{
    /**
     * The annotation preference properties file name.
     */
    private static final String ANNOTATION_PREFERENCE_PROPERTIES_FILE = "annotation.properties";

    private final AnnotationEditorDefaultPreferencesProperties defaultPreferences;
    private final AnnotationSchemaService annotationService;
    private final RepositoryProperties repositoryProperties;
    private final ColoringService coloringService;
    private final AnnotationSchemaProperties annotationEditorProperties;
    private final PreferencesService preferencesService;
    private final UserDao userService;

    public UserPreferencesServiceImpl(
            AnnotationEditorDefaultPreferencesProperties aDefaultPreferences,
            AnnotationSchemaService aAnnotationService, RepositoryProperties aRepositoryProperties,
            ColoringService aColoringService,
            AnnotationSchemaProperties aAnnotationEditorProperties,
            PreferencesService aPreferencesService, UserDao aUserService)
    {
        defaultPreferences = aDefaultPreferences;
        annotationService = aAnnotationService;
        repositoryProperties = aRepositoryProperties;
        coloringService = aColoringService;
        annotationEditorProperties = aAnnotationEditorProperties;
        preferencesService = aPreferencesService;
        userService = aUserService;
    }

    @Override
    public void loadPreferences(AnnotatorState aState, String aSessionOwnerName)
        throws BeansException, IOException
    {
        Validate.notBlank(aSessionOwnerName, "Parameter [sessionOwner] must be specified");

        var preference = loadPreferences(aState.getProject(), aSessionOwnerName, aState.getMode());

        aState.setPreferences(preference);

        // set layers according to preferences
        var allLayers = annotationService.listAnnotationLayer(aState.getProject());
        aState.setAllAnnotationLayers(allLayers);
        aState.setAnnotationLayers(allLayers.stream() //
                .filter(l -> !annotationEditorProperties.isLayerBlocked(l)) //
                .filter(l -> l.isEnabled()) //
                .filter(l -> !preference.getHiddenAnnotationLayerIds().contains(l.getId()))
                .toList());

        // set default layer according to preferences
        var defaultLayer = aState.getAnnotationLayers().stream()
                .filter(layer -> Objects.equals(layer.getId(), preference.getDefaultLayer()))
                .findFirst();

        if (defaultLayer.isPresent()) {
            aState.setDefaultAnnotationLayer(defaultLayer.get());
            aState.setSelectedAnnotationLayer(defaultLayer.get());
        }

        // Make sure the visibility logic of the right sidebar sees if there are selectable layers
        aState.refreshSelectableLayers(annotationEditorProperties::isLayerBlocked);

        if (aState.getDefaultAnnotationLayer() != null) {
            var sessionOwner = userService.getCurrentUser();
            var anchoringPrefs = preferencesService.loadTraitsForUserAndProject(KEY_ANCHORING_MODE,
                    sessionOwner, aState.getProject());
            aState.syncAnchoringModeToDefaultLayer(anchoringPrefs);
        }
    }

    @Override
    public void savePreferences(AnnotatorState aState, String aSessionOwnerName) throws IOException
    {
        savePreferences(aState.getProject(), aSessionOwnerName, aState.getMode(),
                aState.getPreferences());
    }

    @Override
    public synchronized AnnotationPreference loadPreferences(Project aProject,
            String aSessionOwnerName, Mode aMode)
        throws IOException
    {
        Validate.notBlank(aSessionOwnerName, "Parameter [sessionOwner] must be specified");

        // TODO Use modular preference loading once it is available and if there is a corresponding
        // data file. Otherwise, fall back to loading the legacy preferences

        var preferences = loadLegacyPreferences(aProject, aSessionOwnerName, aMode);

        var sessionOwner = userService.get(aSessionOwnerName);

        upgradeLayerVisibilityPreferences(aProject, sessionOwner, preferences);
        upgradeLayoutStatePreferences(aProject, sessionOwner, preferences);

        return preferences;
    }

    private void upgradeLayerVisibilityPreferences(Project aProject, User aSessionOwner,
            AnnotationPreference preferences)
    {
        var maybeLayersState = preferencesService
                .loadOptionalTraitsForUserAndProject(KEY_LAYERS_STATE, aSessionOwner, aProject);

        if (maybeLayersState.isPresent()) {
            var layersState = maybeLayersState.get();
            preferences.setColorPerLayer(layersState.getLayerColoringStrategy());
            preferences.setReadonlyLayerColoringBehaviour(
                    layersState.getReadonlyLayerColoringStrategy());
            preferences.setHiddenAnnotationLayerIds(layersState.getHiddenLayers());
            preferences.setHiddenAnnotationFeatureIds(layersState.getHiddenFeatures());
            preferences.setHiddenTags(layersState.getHiddenFeatureValues());
            return;
        }

        saveLayerVisibilityPreferences(aProject, aSessionOwner, preferences);
    }

    private void saveLayerVisibilityPreferences(Project aProject, User aSessionOwner,
            AnnotationPreference preferences)
    {
        var layersState = preferencesService.loadTraitsForUserAndProject(KEY_LAYERS_STATE,
                aSessionOwner, aProject);
        layersState.setLayerColoringStrategy(preferences.getColorPerLayer());
        layersState
                .setReadonlyLayerColoringStrategy(preferences.getReadonlyLayerColoringBehaviour());
        layersState.setHiddenLayers(preferences.getHiddenAnnotationLayerIds());
        layersState.setHiddenFeatures(preferences.getHiddenAnnotationFeatureIds());
        layersState.setHiddenFeatureValues(preferences.getHiddenTags());
        preferencesService.saveTraitsForUserAndProject(KEY_LAYERS_STATE, aSessionOwner, aProject,
                layersState);
    }

    private void upgradeLayoutStatePreferences(Project aProject, User aSessionOwner,
            AnnotationPreference preferences)
    {
        var maybeLayoutState = preferencesService
                .loadOptionalTraitsForUserAndProject(KEY_LAYOUT_STATE, aSessionOwner, aProject);
        if (maybeLayoutState.isPresent()) {
            var layoutState = maybeLayoutState.get();
            preferences.setSidebarSizeLeft(layoutState.getSidebarSizeLeft());
            preferences.setSidebarSizeRight(layoutState.getSidebarSizeRight());
            return;
        }

        saveLayoutStatePreferences(aProject, aSessionOwner, preferences);
    }

    private void saveLayoutStatePreferences(Project aProject, User aSessionOwner,
            AnnotationPreference preferences)
    {
        var layoutState = preferencesService.loadTraitsForUserAndProject(KEY_LAYOUT_STATE,
                aSessionOwner, aProject);
        layoutState.setSidebarSizeLeft(preferences.getSidebarSizeLeft());
        layoutState.setSidebarSizeRight(preferences.getSidebarSizeRight());
        preferencesService.saveTraitsForUserAndProject(KEY_LAYOUT_STATE, aSessionOwner, aProject,
                layoutState);
    }

    @Override
    public synchronized void savePreferences(Project aProject, String aSessionOwnerName, Mode aMode,
            AnnotationPreference aPref)
        throws IOException
    {
        Validate.notBlank(aSessionOwnerName, "Parameter [sessionOwner] must be specified");

        var sessionOwner = userService.get(aSessionOwnerName);
        Objects.requireNonNull(sessionOwner, "User [" + aSessionOwnerName + "] not found");

        // TODO Switch to a new and modular way of writing preferences
        saveLayerVisibilityPreferences(aProject, sessionOwner, aPref);
        saveLayoutStatePreferences(aProject, sessionOwner, aPref);

        saveLegacyPreferences(aProject, aSessionOwnerName, aMode, aPref);
    }

    /**
     * Save annotation references, such as {@code BratAnnotator#windowSize}..., in a properties file
     * so that they are not required to configure every time they open the document.
     *
     * @param aSessionOwnerName
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
    @Deprecated
    private void saveLegacyPreferences(Project aProject, String aSessionOwnerName, Mode aMode,
            AnnotationPreference aPref)
        throws IOException
    {
        Validate.notBlank(aSessionOwnerName, "Parameter [sessionOwner] must be specified");

        var savedProperties = new HashSet<>(asList( //
                "editor", "fontZoom", "windowSize", "scrollPage", "collapseArcs", "sidebarSizeLeft",
                "sidebarSizeRight", "defaultLayer"));
        var wrapper = PropertyAccessorFactory.forBeanPropertyAccess(aPref);
        var props = new Properties();
        for (var value : wrapper.getPropertyDescriptors()) {
            if (!savedProperties.contains(value.getName())
                    || wrapper.getPropertyValue(value.getName()) == null) {
                continue;
            }

            props.setProperty(aMode + "." + value.getName(),
                    wrapper.getPropertyValue(value.getName()).toString());
        }

        var propertiesPath = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + SETTINGS_FOLDER + "/" + aSessionOwnerName;
        // append existing preferences for the other mode
        if (new File(propertiesPath, ANNOTATION_PREFERENCE_PROPERTIES_FILE).exists()) {
            var properties = loadLegacyPreferencesFile(aSessionOwnerName, aProject);
            for (var entry : properties.entrySet()) {
                var key = entry.getKey().toString();
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

        // try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
        // String.valueOf(aProject.getId()))) {
        // log.info("Saved preferences for user [{}] in project [{}]({})", aUsername,
        // aProject.getName(), aProject.getId());
        // }
    }

    @Deprecated
    private AnnotationPreference loadLegacyPreferences(Project aProject, String aSessionOwnerName,
            Mode aMode)
    {
        Validate.notBlank(aSessionOwnerName, "Parameter [sessionOwner] must be specified");

        var preference = new AnnotationPreference();

        var wrapper = new BeanWrapperImpl(preference);

        // get annotation preference from file system
        try {
            var props = loadLegacyPreferencesFile(aSessionOwnerName, aProject);
            for (var entry : props.entrySet()) {
                var property = entry.getKey().toString();
                var index = property.indexOf(".");
                var propertyName = property.substring(index + 1);
                var mode = property.substring(0, index);
                if (wrapper.isWritableProperty(propertyName) && mode.equals(aMode.getName())) {
                    if (AnnotationPreference.class.getDeclaredField(propertyName)
                            .getGenericType() instanceof ParameterizedType) {
                        if (entry.getValue().toString().startsWith("[")) { // its a list
                            var value = asList(
                                    replaceChars(entry.getValue().toString(), "[]", "").split(","));
                            if (!value.get(0).equals("")) {
                                wrapper.setPropertyValue(propertyName, value);
                            }
                        }
                        else if (entry.getValue().toString().startsWith("{")) { // its a map
                            var s = replaceChars(entry.getValue().toString(), "{}", "");
                            var value = Arrays.stream(s.split(",")).map(x -> x.split("="))
                                    .collect(toMap(x -> x[0], x -> x[1]));
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
            preference.setHiddenAnnotationFeatureIds(new HashSet<>());
            preference.setHiddenTags(new HashMap<>());
            preference.setWindowSize(preferencesService
                    .loadDefaultTraitsForProject(KEY_BRAT_EDITOR_MANAGER_PREFS, aProject)
                    .getDefaultPageSize());
            preference.setScrollPage(defaultPreferences.isAutoScroll());
        }

        // Get color preferences for each layer, init with default if not found
        var colorPerLayer = preference.getColorPerLayer();
        if (colorPerLayer == null) {
            colorPerLayer = new HashMap<>();
            preference.setColorPerLayer(colorPerLayer);
        }

        for (var layer : annotationService.listAnnotationLayer(aProject)) {
            if (!colorPerLayer.containsKey(layer.getId())) {
                colorPerLayer.put(layer.getId(), coloringService.getBestInitialStrategy(layer));
            }
        }

        // Upgrade from single sidebar width setting to split setting
        if (preference.getSidebarSizeLeft() == 0 && preference.getSidebarSizeRight() == 0) {
            preference.setSidebarSizeLeft(preference.getSidebarSize());
            preference.setSidebarSizeRight(preference.getSidebarSize());
        }

        return preference;
    }

    /**
     * Load annotation preferences from a property file.
     *
     * @param aSessionOwnerName
     *            the user name.
     * @param aProject
     *            the project where the user is working on.
     * @return the properties.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private Properties loadLegacyPreferencesFile(String aSessionOwnerName, Project aProject)
        throws IOException
    {
        var properties = new Properties();
        properties
                .load(new FileInputStream(new File(repositoryProperties.getPath().getAbsolutePath()
                        + "/" + PROJECT_FOLDER + "/" + aProject.getId() + "/" + SETTINGS_FOLDER
                        + "/" + aSessionOwnerName + "/" + ANNOTATION_PREFERENCE_PROPERTIES_FILE)));
        return properties;
    }

    /**
     * @deprecated We have this only so we can read the default page size here...
     */
    @Deprecated
    public static final PreferenceKey<BratAnnotationEditorManagerPrefs> KEY_BRAT_EDITOR_MANAGER_PREFS = //
            new PreferenceKey<>(BratAnnotationEditorManagerPrefs.class,
                    "annotation/editor/brat/manager");

    /**
     * @deprecated We have this only so we can read the default page size here...
     */
    @Deprecated
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BratAnnotationEditorManagerPrefs
        implements PreferenceValue
    {
        private static final long serialVersionUID = 8809856241481077303L;

        private int defaultPageSize = 20;

        public BratAnnotationEditorManagerPrefs()
        {
            var defaults = ApplicationContextProvider.getApplicationContext()
                    .getBean(AnnotationEditorDefaultPreferencesProperties.class);
            defaultPageSize = defaults.getPageSize();
        }

        public int getDefaultPageSize()
        {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int aDefaultPageSize)
        {
            defaultPageSize = aDefaultPageSize;
        }
    }
}
