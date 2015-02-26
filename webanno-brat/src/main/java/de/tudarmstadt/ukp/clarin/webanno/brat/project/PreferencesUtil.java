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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * This class contains Utility methods that can be used in Project settings
 *
 * @author Seid Muhie Yimam
 *
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
}
