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
package de.tudarmstadt.ukp.inception.annotation.feature.multistring;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportProperties;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationSchemaServiceAutoConfiguration#multiValueStringFeatureSupport}.
 * </p>
 */
public class MultiValueStringFeatureSupport
    extends UimaPrimitiveFeatureSupport_ImplBase<MultiValueStringFeatureTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<FeatureType> primitiveTypes;

    private final StringFeatureSupportProperties properties;

    private final AnnotationSchemaService schemaService;

    /*
     * Constructor for use in unit tests to avoid having to always instantiate the properties.
     */
    public MultiValueStringFeatureSupport()
    {
        this(new StringFeatureSupportPropertiesImpl(), null);
    }

    public MultiValueStringFeatureSupport(StringFeatureSupportProperties aProperties,
            AnnotationSchemaService aSchemaService)
    {
        properties = aProperties;
        schemaService = aSchemaService;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        primitiveTypes = asList(new FeatureType(TYPE_NAME_STRING_ARRAY,
                "Primitive: String (multi-valued)", getId()));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return Collections.unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        return MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())
                && CAS.TYPE_NAME_STRING_ARRAY.equals(aFeature.getType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof String) {
            return asList((String) aValue);
        }

        if (aValue instanceof List) {
            return (List<String>) aValue;
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public Serializable wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof StringArray value) {
            return new ArrayList<>(asList(value.toArray()));
        }

        if (aValue instanceof String value) {
            return new ArrayList<>(asList(value));
        }

        if (aValue instanceof Collection) {
            return (Serializable) aValue;
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws IllegalFeatureValueException
    {
        if (!accepts(aFeature)) {
            throw unsupportedFeatureTypeException(aFeature);
        }

        var fs = getFS(aCas, aFeature, aAddress);
        var values = unwrapFeatureValue(aFeature, fs.getCAS(), aValue);
        if (values == null || values.isEmpty()) {
            FSUtil.setFeature(fs, aFeature.getName(), (Collection<String>) null);
            return;
        }

        for (String value : values) {
            schemaService.createMissingTag(aFeature, value);
        }

        // Create a new array if size differs otherwise re-use existing one
        var array = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);
        if (array == null || (array.size() != values.size())) {
            array = fs.getCAS().createStringArrayFS(values.size());
        }

        // Fill in links
        array.copyFromArray(values.toArray(new String[values.size()]), 0, 0, values.size());

        fs.setFeatureValue(fs.getType().getFeatureByBaseName(aFeature.getName()), array);
    }

    @Override
    public void pushFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        if (!accepts(aFeature)) {
            throw unsupportedFeatureTypeException(aFeature);
        }

        var fs = getFS(aCas, aFeature, aAddress);
        var newValues = unwrapFeatureValue(aFeature, fs.getCAS(), aValue);
        if (newValues == null || newValues.isEmpty()) {
            return;
        }

        for (String value : newValues) {
            schemaService.createMissingTag(aFeature, value);
        }

        var feature = fs.getType().getFeatureByBaseName(aFeature.getName());
        var oldValues = (Collection<String>) wrapFeatureValue(aFeature, fs.getCAS(),
                fs.getFeatureValue(feature));

        var mergedValues = new LinkedHashSet<String>();
        if (oldValues != null) {
            mergedValues.addAll(oldValues);
        }
        mergedValues.addAll(newValues);

        // Create a new array if size differs otherwise re-use existing one
        var array = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);
        if (array == null || (array.size() != mergedValues.size())) {
            array = fs.getCAS().createStringArrayFS(mergedValues.size());
        }

        array.copyFromArray(mergedValues.toArray(new String[mergedValues.size()]), 0, 0,
                mergedValues.size());

        fs.setFeatureValue(feature, array);
    }

    @Override
    public boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        if (aFeature.isRequired()) {
            var value = FSUtil.getFeature(aFS, aFeature.getName(), List.class);
            return isNotEmpty(value);
        }

        return true;
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new MultiValueStringFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        aFeature.setMode(MultiValueMode.ARRAY);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        return new MultiSelectTextFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
    }

    @Override
    public MultiValueStringFeatureTraits createDefaultTraits()
    {
        return new MultiValueStringFeatureTraits();
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        Feature labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());

        if (labelFeature == null) {
            return null;
        }

        List<String> values = getFeatureValue(aFeature, aFs);
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream().collect(Collectors.joining(", "));
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        var results = new VLazyDetailGroup();
        if (aValue instanceof Iterable) {
            var values = (Iterable<?>) aValue;
            for (var v : values) {
                if (v instanceof String value) {
                    var tag = schemaService.getTag(value, aFeature.getTagset());

                    if (isNotBlank(value) && aFeature.getTagset() != null && tag.isEmpty()) {
                        results.addDetail(new VLazyDetail(value, "Tag not in tagset"));
                    }

                    if (tag.map(t -> isNotBlank(t.getDescription())).orElse(false)) {
                        results.addDetail(new VLazyDetail(value, tag.get().getDescription()));
                    }
                }
            }
        }
        return asList(results);
    }
}
