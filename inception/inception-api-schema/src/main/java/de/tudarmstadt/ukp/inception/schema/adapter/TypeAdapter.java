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
package de.tudarmstadt.ukp.inception.schema.adapter;

import static de.tudarmstadt.ukp.inception.schema.adapter.AnnotationComparisonUtils.isEquivalentSpanAnnotation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

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
     * @param cas
     *            the CAS.
     * @return the type.
     */
    default Type getAnnotationType(CAS cas)
    {
        return CasUtil.getType(cas, getAnnotationTypeName());
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
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aCas
     *            the CAS object
     * @param aVid
     *            the VID of the object to be deleted.
     */
    void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid);

    /**
     * @return the layer for which this adapter has been created.
     */
    AnnotationLayer getLayer();

    /**
     * @return the features defined for this layer.
     * 
     * @see AnnotationSchemaService#listAnnotationFeature(AnnotationLayer)
     */
    Collection<AnnotationFeature> listFeatures();

    /**
     * Set the value of the given feature.
     * 
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
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
    void setFeatureValue(SourceDocument aDocument, String aUsername, CAS aCas, int aAddress,
            AnnotationFeature aFeature, Object aValue)
        throws AnnotationException;

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
     * Disable the adapter from dispatching any events. This is useful for backend bulk operations
     * that should not be tracked in detail.
     */
    void silenceEvents();

    Selection select(VID aVid, AnnotationFS aAnnotation);

    /**
     * @return the encoded type name sent to the browser.
     * @see #decodeTypeName(String)
     */
    default String getEncodedTypeName()
    {
        return TypeUtil.getUiTypeName(getLayer());
    }

    /**
     * @param aType
     *            a encoded type name from {@link #getEncodedTypeName()}
     * @return the layer ID.
     * @see #getEncodedTypeName()
     */
    static long decodeTypeName(String aType)
    {
        return TypeUtil.getLayerId(aType);
    }

    <T> Optional<T> getTraits(Class<T> aInterface);

    default boolean equivalents(AnnotationFS aFs1, AnnotationFS aFs2)
    {
        return equivalents(aFs1, aFs2, null);
    }

    default boolean equivalents(AnnotationFS aFs1, AnnotationFS aFs2, FeatureFilter aFilter)
    {
        return isEquivalentSpanAnnotation(aFs1, aFs2, aFilter);
    }
}
