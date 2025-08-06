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
package de.tudarmstadt.ukp.inception.schema.api.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeatureFilter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * A type adapter encapsulates a specific kind of annotation layer, e.g. spans, relations or chains.
 * A type adapter can be obtained via {@link AnnotationSchemaService#getAdapter} and is always
 * created for one concrete layer (e.g. POS, NamedEntity). It allows interacting with annotations on
 * the layer, e.g. creating annotations, deleting annotations, setting and getting feature values.
 * Most actions are defined by implementations of this interface such as {@code SpanAdapter},
 * {@code RelationAdapter} or {@code ChainAdapter}.
 */
public interface TypeAdapter
{
    /**
     * String used to separate multiple values in the annotation label displayed in the UI.
     */
    String FEATURE_SEPARATOR = " | ";

    default long getTypeId()
    {
        return getLayer().getId();
    }

    /**
     * Get the CAS type of the this {@link TypeAdapter}
     *
     * @param aCas
     *            the CAS.
     * @return the type.
     */
    default Optional<Type> getAnnotationType(CAS aCas)
    {
        return Optional.ofNullable(aCas.getTypeSystem().getType(getAnnotationTypeName()));
    }

    /**
     * Get the CAS type of the this {@link TypeAdapter}
     *
     * @return the type.
     */
    default public String getAnnotationTypeName()
    {
        return getLayer().getName();
    }

    /**
     * If the underlying layer is attached to another layer, get the name of the feature in the
     * other layer which is used to refer to annotations on the underlying layer. For example, every
     * annotation on the {@code POS} layer must correspond to an annotation on the {@link Token}
     * layer and the token feature {@code Token.getPos()} must point the POS annotation. Thus, for
     * the POS layer, this method would return {@code pos} and the method
     * {@link #getAttachTypeName()} would return
     * {@code de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token}.
     *
     * @return the attach feature name.
     */
    String getAttachFeatureName();

    /**
     * @return the name of the layer the current layer attaches to (if any)
     * 
     * @see #getAttachFeatureName()
     */
    String getAttachTypeName();

    /**
     * Delete a annotation from CAS.
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aDocumentOwner
     *            the user to which the CAS belongs
     * @param aCas
     *            the CAS object
     * @param aVid
     *            the VID of the object to be deleted.
     */
    void delete(SourceDocument aDocument, String aDocumentOwner, CAS aCas, VID aVid)
        throws AnnotationException;

    /**
     * @return the layer for which this adapter has been created.
     */
    AnnotationLayer getLayer();

    /**
     * @return the features defined for this layer.
     * 
     * @see AnnotationSchemaService#listSupportedFeatures(AnnotationLayer)
     */
    Collection<AnnotationFeature> listFeatures();

    Optional<AnnotationFeature> getFeature(String aName);

    /**
     * Set the value of the given feature.
     * 
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aDocumentOwner
     *            the user to which the CAS belongs
     * @param aCas
     *            the CAS.
     * @param aAddress
     *            the annotation ID.
     * @param aFeature
     *            the feature.
     * @param aValue
     *            the value.
     * @throws AnnotationException
     *             if there was an error setting the feature value
     */
    void setFeatureValue(SourceDocument aDocument, String aDocumentOwner, CAS aCas, int aAddress,
            AnnotationFeature aFeature, Object aValue)
        throws AnnotationException;

    default void setFeatureValue(SourceDocument aDocument, String aDocumentOwner,
            FeatureStructure aFs, AnnotationFeature aFeature, Object aValue)
        throws AnnotationException

    {
        setFeatureValue(aDocument, aDocumentOwner, aFs.getCAS(), aFs.getAddress(), aFeature,
                aValue);
    }

    void pushFeatureValue(SourceDocument aDocument, String aUsername, CAS aCas, int aAddress,
            AnnotationFeature aFeature, Object aValue)
        throws AnnotationException;

    default void pushFeatureValue(SourceDocument aDocument, String aDocumentOwner,
            FeatureStructure aFs, AnnotationFeature aFeature, Object aValue)
        throws AnnotationException

    {
        pushFeatureValue(aDocument, aDocumentOwner, aFs.getCAS(), aFs.getAddress(), aFeature,
                aValue);
    }

    /**
     * Get the value of the given feature.
     * 
     * @param aFeature
     *            the feature.
     * @param aFs
     *            the annotation.
     * @return the feature value.
     */
    <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs);

    FeatureState getFeatureState(AnnotationFeature aFeature, FeatureStructure aFs);

    boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS);

    /**
     * Initialize the layer when it is created. This can be used e.g. to add default features. This
     * is mainly called when a layer is created through the UI, in other cases (e.g. during import)
     * all necessary information should be included in the imported data.
     * 
     * @param aSchemaService
     *            the schema service (because type adapters do currently not support dependency
     *            injection)
     */
    void initializeLayerConfiguration(AnnotationSchemaService aSchemaService);

    /**
     * Check if all annotations of this layer conform with the layer configuration. This is usually
     * called when a document is marked as finished to prevent invalid annotations ending up in the
     * finished document.
     * 
     * @param aCas
     *            the CAS to validate
     * @return a list of messages indicating the result of the validation
     */
    List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas);

    /**
     * Disable the adapter from dispatching any events. This is useful for back-end bulk operations
     * that should not be tracked in detail.
     */
    void silenceEvents();

    /**
     * @return if events are silenced.
     */
    boolean isSilenced();

    /**
     * @deprecated Use {@link #publishEvent(Supplier)} instead. This allows to not initialize events
     *             that are not sent.
     */
    @Deprecated
    void publishEvent(ApplicationEvent aEvent);

    void publishEvent(Supplier<ApplicationEvent> aEventSupplier);

    Selection select(VID aVid, AnnotationFS aAnnotation);

    <T> Optional<T> getTraits(Class<T> aInterface);

    <T> Optional<T> getFeatureTraits(AnnotationFeature aFeature, Class<T> aInterface);

    default boolean isEquivalentAnnotation(FeatureStructure aFS1, FeatureStructure aFS2)
    {
        if (!isSamePosition(aFS1, aFS2)) {
            return false;
        }

        for (var feature : listFeatures()) {
            if (!isFeatureValueEqual(feature, aFS1, aFS2)) {
                return false;
            }
        }

        return true;
    }

    default int countNonEqualFeatures(AnnotationFS aFS1, AnnotationFS aFS2,
            AnnotationFeatureFilter aFilter)
    {
        return (int) listFeatures().stream() //
                .filter(feature -> aFilter == null
                        || (aFilter.isAllowed(aFS1, feature) && aFilter.isAllowed(aFS1, feature)))
                .filter(feature -> !isFeatureValueEqual(feature, aFS1, aFS2)) //
                .count();
    }

    <T> Optional<FeatureSupport<T>> getFeatureSupport(String aName);

    <T> Optional<FeatureSupport<T>> getFeatureSupport(AnnotationFeature aFeature);

    String renderFeatureValue(FeatureStructure aFS, String aFeature);

    boolean isFeatureValueEqual(AnnotationFeature aFeature, FeatureStructure aFS1,
            FeatureStructure aFS2);

    boolean isSamePosition(FeatureStructure aFS1, FeatureStructure aFS2);
}
