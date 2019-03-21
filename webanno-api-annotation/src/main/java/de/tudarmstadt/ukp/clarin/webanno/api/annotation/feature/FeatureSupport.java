/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Extension point for new types of annotation features. 
 * 
 * @param <T> the traits type. If no traits are supported, this should be {@link Void}.
 */
public interface FeatureSupport<T>
    extends BeanNameAware
{
    String getId();
    
    /**
     * Checks whether the given feature is provided by the current feature support.
     * 
     * @param aFeature
     *            a feature definition.
     * @return whether the given feature is provided by the current feature support.
     */
    boolean accepts(AnnotationFeature aFeature);
    
    /**
     * Get the feature type for the given annotation feature. If the current feature support does
     * not provide any feature type for the given feature, an empty value is returned. As we
     * usually use {@link FeatureType} objects in feature type selection lists, this method is
     * helpful in obtaining the selected value of such a list from the {@link AnnotationFeature}
     * object being edited.
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
     * Called when the user selects a feature in the feature detail form. It allows the feature
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
     * Read the traits for the given {@link AnnotationFeature}. If traits are supported, then this
     * method must be overwritten. A typical implementation would read the traits from a JSON string
     * stored in {@link AnnotationFeature#getTraits}, but it would also possible to load the
     * traits from a database table.
     * 
     * @param aFeature
     *            the feature whose traits should be obtained.
     * @return the traits.
     */
    default T readTraits(AnnotationFeature aFeature)
    {
        return null;
    }

    /**
     * Write the traits for the given {@link AnnotationFeature}. If traits are supported, then this
     * method must be overwritten. A typical implementation would write the traits from to JSON
     * string stored in {@link AnnotationFeature#setTraits}, but it would also possible to store
     * the traits from a database table.
     * 
     * @param aFeature
     *            the feature whose traits should be written.
     * @param aTraits
     *            the traits.
     */
    default void writeTraits(AnnotationFeature aFeature, T aTraits)
    {
        aFeature.setTraits(null);
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
     * <b>NOTE:</b> If this method should never be overwritten! Overwrite
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
        Feature labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());
        return renderFeatureValue(aFeature, aFs.getFeatureValueAsString(labelFeature));
    }

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
     * Update this feature with a new value. This method should not be called directly but
     * rather via {@link TypeAdapter#setFeatureValue}.
     * <p>
     * Normally, this method accepts a primitive UIMA-supported type as value. However, if may
     * also accept a different type which needs to be converted to a primitive UIMA-supported
     * type. If this is the case, the method should also always still accept a value of the
     * primitive type to which the value is converted. For example, if the method accepts a
     * {@code Pair<Integer, String>} and then stores the integer key to the CAS, then it should
     * also accept {@code Integer} values.
     *
     * @param aJcas
     *            the JCas.
     * @param aFeature
     *            the feature.
     * @param aAddress
     *            the annotation ID.
     * @param aValue
     *            the value.
     */
    default void setFeatureValue(CAS aJcas, AnnotationFeature aFeature, int aAddress,
            Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        
        Object value = unwrapFeatureValue(aFeature, fs.getCAS(), aValue);
        setFeature(fs, aFeature, value);
    }

    default <V> V getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Object value;
        
        Feature f = aFS.getType().getFeatureByBaseName(aFeature.getName());
        if (f.getRange().isPrimitive()) {
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

    /**
     * Convert the value returned by the feature editor to the value stored in the CAS.
     * 
     * @param aFeature
     *            the feature.
     * @param aValue
     *            the value provided from the feature editor.
     * @return the CAS value.
     */
    <V> V  unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue);

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
    Object wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue);
    
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

    default IllegalArgumentException unsupportedMultiValueModeException(
            AnnotationFeature aFeature)
    {
        return new IllegalArgumentException("Unsupported multi-value mode ["
                + aFeature.getMultiValueMode() + "] on feature [" + aFeature.getName() + "]");
    }
    
    /**
     * @deprecated Use {@link #unsupportedFeatureTypeException(AnnotationFeature)} instead.
     */
    @Deprecated
    default IllegalArgumentException unsupportedFeatureTypeException(FeatureState aFeatureState)
    {
        return unsupportedFeatureTypeException(aFeatureState.feature);
    }

    /**
     * @deprecated Use {@link #unsupportedLinkModeException(AnnotationFeature)} instead.
     */
    @Deprecated
    default IllegalArgumentException unsupportedLinkModeException(FeatureState aFeatureState)
    {
        return unsupportedLinkModeException(aFeatureState.feature);
    }
    
    /**
     * @deprecated Use {@link #unsupportedMultiValueModeException(AnnotationFeature)} instead.
     */
    @Deprecated
    default IllegalArgumentException unsupportedMultiValueModeException(FeatureState aFeatureState)
    {
        return unsupportedMultiValueModeException(aFeatureState.feature);
    }
}
