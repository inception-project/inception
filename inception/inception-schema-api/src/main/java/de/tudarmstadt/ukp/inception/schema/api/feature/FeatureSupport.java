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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import static de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService.FEATURE_SUFFIX_SEP;
import static de.tudarmstadt.ukp.inception.schema.api.feature.FeatureUtil.setFeature;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * Extension point for new types of annotation features.
 * 
 * @param <T>
 *            the traits type. If no traits are supported, this should be {@link Void}.
 */
public interface FeatureSupport<T>
    extends BeanNameAware, Extension<AnnotationFeature>
{
    String SUFFIX_SUGGESTION_INFO = FEATURE_SUFFIX_SEP + "suggestionInfo";

    @Override
    String getId();

    /**
     * Checks whether the given feature is provided by the current feature support.
     * 
     * @param aFeature
     *            a feature definition.
     * @return whether the given feature is provided by the current feature support.
     */
    @Override
    boolean accepts(AnnotationFeature aFeature);

    /**
     * Get the feature type for the given annotation feature. If the current feature support does
     * not provide any feature type for the given feature, an empty value is returned. As we usually
     * use {@link FeatureType} objects in feature type selection lists, this method is helpful in
     * obtaining the selected value of such a list from the {@link AnnotationFeature} object being
     * edited.
     * 
     * @param aAnnotationFeature
     *            an annotation feature.
     * @return the corresponding feature type.
     */
    default Optional<FeatureType> getFeatureType(AnnotationFeature aAnnotationFeature)
    {
        return getSupportedFeatureTypes(aAnnotationFeature.getLayer()).stream()
                .filter(t -> t.getName().equals(aAnnotationFeature.getType())).findFirst();
    }

    /**
     * Get a list of feature types provided by this feature support. These are added to the list of
     * feature types a user can choose from when creating a new feature through the layer settings
     * user interface. The feature types returned here consist of a human-readable name as well as
     * an internal feature type name.
     * 
     * @param aAnnotationLayer
     *            an annotation layer definition.
     * @return a list of the supported features.
     */
    List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer);

    /**
     * Generate a UIMA feature definition for the given feature into the provided type definition.
     * 
     * @param aTSD
     *            the target type system description.
     * @param aTD
     *            the target type description.
     * @param aFeature
     *            the feature definition.
     */
    void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature);

    /**
     * Called when the user saves a feature in the feature detail form. It allows the feature
     * support to fill in settings which are not configurable through the UI, e.g. link feature
     * details.
     * 
     * @param aFeature
     *            a feature definition.
     */
    default void configureFeature(AnnotationFeature aFeature)
    {
        // Nothing to do
    }

    /**
     * Returns a Wicket component to configure the specific traits of this feature type. Note that
     * every {@link FeatureSupport} has to return a <b>different class</b> here. So it is not
     * possible to simple return a Wicket {@link Panel} here, but it must be a subclass of
     * {@link Panel} used exclusively by the current {@link FeatureSupport}. If this is not done,
     * then the traits editor in the UI will not be correctly updated when switching between feature
     * types!
     * 
     * @param aId
     *            a markup ID.
     * @param aFeatureModel
     *            a model holding the annotation feature for which the traits editor should be
     *            created.
     * @return the traits editor component .
     */
    default Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new EmptyPanel(aId);
    }

    /**
     * @return a traits object with default values. If traits are supported, then this method must
     *         be overwritten.
     */
    default T createDefaultTraits()
    {
        return null;
    }

    /**
     * Read the traits for the given {@link AnnotationFeature}. The default implementation reads the
     * traits from a JSON string stored in {@link AnnotationFeature#getTraits}, but it would also
     * possible to load the traits from a database table.
     * 
     * @param aFeature
     *            the feature whose traits should be obtained.
     * @return the traits.
     */
    @SuppressWarnings("unchecked")
    default T readTraits(AnnotationFeature aFeature)
    {
        // Obtain a template traits object from which we can obtain the class
        var traits = createDefaultTraits();
        if (traits == null) {
            return null;
        }

        // Try loading the traits from the feature
        try {
            traits = (T) JSONUtil.fromJsonString(traits.getClass(), aFeature.getTraits());
        }
        catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Unable to read traits", e);
        }

        // If there were no traits or there was an error loading them, use the default traits
        if (traits == null) {
            traits = createDefaultTraits();
        }

        return traits;
    }

    /**
     * Write the traits for the given {@link AnnotationFeature}. The default implementation writes
     * the traits from to JSON string stored in {@link AnnotationFeature#setTraits}, but it would
     * also possible to store the traits from a database table.
     * 
     * @param aFeature
     *            the feature whose traits should be written.
     * @param aTraits
     *            the traits.
     */
    default void writeTraits(AnnotationFeature aFeature, T aTraits)
    {
        var traitsTemplate = createDefaultTraits();
        if (traitsTemplate == null) {
            return;
        }

        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Unable to write traits", e);
        }
    }

    /**
     * Create a feature value editor for use in the annotation detail editor pane and similar
     * locations.
     * 
     * @param aId
     *            the component id.
     * @param aOwner
     *            an enclosing component which may contain other feature editors. If actions are
     *            performed which may affect other feature editors, e.g because of constraints
     *            rules, then these need to be re-rendered. This is done by requesting a
     *            re-rendering of the enclosing component.
     * @param aHandler
     *            to allow the editor to perform typical annotation actions.
     * @param aStateModel
     *            provides access to the state of the annotation editor.
     * @param aFeatureStateModel
     *            provides access to the state of the feature being edited.
     * @return an editor component.
     */
    FeatureEditor createEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
            IModel<AnnotatorState> aStateModel, IModel<FeatureState> aFeatureStateModel);

    /**
     * Gets the label that should be displayed for the given feature value in the UI. {@code null}
     * is an acceptable return value for this method.
     * 
     * @param aFeature
     *            the feature to be rendered.
     * @param aLabel
     *            the internal label.
     * @return the UI label.
     */
    default String renderFeatureValue(AnnotationFeature aFeature, String aLabel)
    {
        return aLabel;
    }

    /**
     * Gets the label values that should be displayed for the given feature value in the UI.
     * {@code null} is an acceptable return value for this method.
     * 
     * <b>NOTE:</b> In most cases, it is better to overwrite
     * {@link #renderFeatureValue(AnnotationFeature, String) instead}.
     * 
     * @param aFeature
     *            the feature to be rendered.
     * @param aFs
     *            the feature structure from which to obtain the label.
     * @return the UI label.
     */
    default List<String> renderFeatureValues(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        var labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());
        if (labelFeature == null) {
            return emptyList();
        }

        var featureValue = renderFeatureValue(aFeature, aFs.getFeatureValueAsString(labelFeature));
        if (featureValue == null) {
            return emptyList();
        }

        return asList(featureValue);
    }

    /**
     * Gets the label that should be displayed for the given feature value in the UI. {@code null}
     * is an acceptable return value for this method.
     * 
     * <b>NOTE:</b> In most cases, it is better to overwrite
     * {@link #renderFeatureValue(AnnotationFeature, String) instead}.
     * 
     * @param aFeature
     *            the feature to be rendered.
     * @param aFs
     *            the feature structure from which to obtain the label.
     * @return the UI label.
     */
    default String renderFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        var values = renderFeatureValues(aFeature, aFs);
        if (values.isEmpty()) {
            return null;
        }

        return join(", ", renderFeatureValues(aFeature, aFs));
    }

    default List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        return emptyList();
    }

    /**
     * Update this feature with a new value. This method should not be called directly but rather
     * via {@link TypeAdapter#setFeatureValue}.
     * <p>
     * Normally, this method accepts a primitive UIMA-supported type as value. However, if may also
     * accept a different type which needs to be converted to a primitive UIMA-supported type. If
     * this is the case, the method should also always still accept a value of the primitive type to
     * which the value is converted. For example, if the method accepts a
     * {@code Pair<Integer, String>} and then stores the integer key to the CAS, then it should also
     * accept {@code Integer} values.
     *
     * @param aCas
     *            the CAS.
     * @param aFeature
     *            the feature.
     * @param aAddress
     *            the annotation ID.
     * @param aValue
     *            the value.
     * @throws AnnotationException
     *             if there was a problem setting the feature value
     */
    default void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        if (!accepts(aFeature)) {
            throw unsupportedFeatureTypeException(aFeature);
        }

        var fs = selectFsByAddr(aCas, aAddress);

        var value = unwrapFeatureValue(aFeature, aValue);
        setFeature(fs, aFeature, value);
    }

    default void pushFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        setFeatureValue(aCas, aFeature, aAddress, aValue);
    }

    @SuppressWarnings("unchecked")
    default <V> V getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Object value;

        var f = aFS.getType().getFeatureByBaseName(aFeature.getName());

        if (f == null) {
            value = getNullFeatureValue(aFeature, aFS);
        }
        else if (f.getRange().isPrimitive()) {
            value = FSUtil.getFeature(aFS, aFeature.getName(), Object.class);
        }
        else if (FSUtil.isMultiValuedFeature(aFS, f)) {
            value = FSUtil.getFeature(aFS, aFeature.getName(), List.class);
        }
        else {
            value = FSUtil.getFeature(aFS, aFeature.getName(), FeatureStructure.class);
        }

        return (V) wrapFeatureValue(aFeature, aFS.getCAS(), value);
    }

    <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS);

    default boolean isFeatureValueEqual(AnnotationFeature aFeature, FeatureStructure aFS1,
            FeatureStructure aFS2)
    {
        var value1 = getFeatureValue(aFeature, aFS1);
        var value2 = getFeatureValue(aFeature, aFS2);
        return Objects.equals(value1, value2);
    }

    default void clearFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        setFeature(aFS, aFeature, null);
    }

    default boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return true;
    }

    /**
     * Convert the value returned by the feature editor to the value stored in the CAS.
     * 
     * @param <V>
     *            the value type
     * @param aFeature
     *            the feature.
     * @param aValue
     *            the value provided from the feature editor.
     * @return the CAS value.
     */
    <V> V unwrapFeatureValue(AnnotationFeature aFeature, Object aValue);

    /**
     * Convert a CAS representation of the feature value to the type of value which the feature
     * editor expects.
     * 
     * @param aFeature
     *            the feature.
     * @param aCAS
     *            the CAS mainly for access to the type system.
     * @param aValue
     *            string representation of the value
     * @return feature editor representation of the value.
     */
    Serializable wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue);

    default void initializeAnnotation(AnnotationFeature aFeature, FeatureStructure aFS)
        throws AnnotationException
    {
        // Nothing by default
    }

    default IllegalArgumentException unsupportedFeatureTypeException(AnnotationFeature aFeature)
    {
        return new IllegalArgumentException("Unsupported type [" + aFeature.getType()
                + "] on feature [" + aFeature.getName() + "]");
    }

    default IllegalArgumentException unsupportedLinkModeException(AnnotationFeature aFeature)
    {
        return new IllegalArgumentException("Unsupported link mode [" + aFeature.getLinkMode()
                + "] on feature [" + aFeature.getName() + "]");
    }

    default IllegalArgumentException unsupportedMultiValueModeException(AnnotationFeature aFeature)
    {
        return new IllegalArgumentException("Unsupported multi-value mode ["
                + aFeature.getMultiValueMode() + "] on feature [" + aFeature.getName() + "]");
    }

    /**
     * @deprecated Use {@link #unsupportedFeatureTypeException(AnnotationFeature)} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    default IllegalArgumentException unsupportedFeatureTypeException(FeatureState aFeatureState)
    {
        return unsupportedFeatureTypeException(aFeatureState.feature);
    }

    /**
     * @deprecated Use {@link #unsupportedLinkModeException(AnnotationFeature)} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    default IllegalArgumentException unsupportedLinkModeException(FeatureState aFeatureState)
    {
        return unsupportedLinkModeException(aFeatureState.feature);
    }

    /**
     * @deprecated Use {@link #unsupportedMultiValueModeException(AnnotationFeature)} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    default IllegalArgumentException unsupportedMultiValueModeException(FeatureState aFeatureState)
    {
        return unsupportedMultiValueModeException(aFeatureState.feature);
    }

    /**
     * By default, the annotation editors should receive a focus if possible. However, in some
     * situations, doing this is contraproductive, e.g. if single-key shortcuts are defined. These
     * shortcuts should not trigger if an input field is in focus because otherwise one could never
     * ever enter something into an input field. But that also means that if an editor gets the
     * focus automatically, then the shortcuts won't be available and the user has to unfocus the
     * editor before the shortcuts can be used. So it is better to suppress the auto-focus in such a
     * case.
     * 
     * @param aFeature
     *            the feature
     * @return if auto-focusing the feature editor should be suppressed
     */
    default boolean suppressAutoFocus(AnnotationFeature aFeature)
    {
        return false;
    }

    default FeatureStructure getFS(CAS aCas, AnnotationFeature aFeature, int aAddress)
    {
        var fs = selectFsByAddr(aCas, aAddress);
        var feature = fs.getType().getFeatureByBaseName(aFeature.getName());

        if (feature == null) {
            throw new IllegalArgumentException("On [" + fs.getType().getName() + "] the feature ["
                    + aFeature.getName() + "] does not exist.");
        }
        return fs;
    }

    default boolean isUsingDefaultOptions(AnnotationFeature aFeature)
    {
        return true;
    }

    /**
     * @return Whether the given feature is accessible. Non-accessible feature do not need to
     *         support {@link #getFeatureValue}, {@link #setFeatureValue}, {@link #wrapFeatureValue}
     *         and {@link #unwrapFeatureValue}.
     * @param aFeature
     *            the feature
     * @see FeatureSupportRegistry#isAccessible(AnnotationFeature)
     */
    default boolean isAccessible(AnnotationFeature aFeature)
    {
        return true;
    }

    /**
     * @return whether the feature value should be copied when the owning annotation is merged into
     *         another CAS.
     */
    default boolean isCopyOnCurationMerge(AnnotationFeature aFeature)
    {
        return true;
    }

    default void pushSuggestions(SourceDocument aDocument, String aDataOwner,
            AnnotationBaseFS aAnnotation, AnnotationFeature aFeature,
            List<SuggestionState> aSuggestions)
    {
        // No default implementation;
    }

    default List<SuggestionState> getSuggestions(FeatureStructure aAnnotation,
            AnnotationFeature aFeature)
    {
        // No default implementation
        return Collections.emptyList();
    }

    default String renderWrappedFeatureValue(Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof Iterable multiValue) {
            return String.join(", ", multiValue);
        }

        return aValue.toString();
    }
}
