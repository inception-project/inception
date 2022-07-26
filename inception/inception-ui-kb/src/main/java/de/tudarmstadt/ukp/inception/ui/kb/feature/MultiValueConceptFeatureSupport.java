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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalFeatureValueException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.MultiValueConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailQuery;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailResult;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureType;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;

/**
 * Extension providing knowledge-base-related features for annotations.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#multiValueConceptFeatureSupport}.
 * </p>
 */
public class MultiValueConceptFeatureSupport
    implements FeatureSupport<MultiValueConceptFeatureTraits>
{
    private static final Logger LOG = LoggerFactory
            .getLogger(MultiValueConceptFeatureSupport.class);

    public static final String PREFIX = "kb-multi:";

    public static final String ANY_OBJECT = "<ANY>";
    public static final String TYPE_ANY_OBJECT = PREFIX + ANY_OBJECT;

    private final ConceptLabelCache labelCache;

    private String featureSupportId;

    @Autowired
    public MultiValueConceptFeatureSupport(ConceptLabelCache aLabelCache)
    {
        labelCache = aLabelCache;
    }

    @Override
    public String getId()
    {
        return featureSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        featureSupportId = aBeanName;
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            return false;
        case ARRAY:
            return aFeature.getType().startsWith(PREFIX);
        default:
            return false;
        }
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        // We just start with no specific scope at all (ANY) and let the user refine this via
        // the traits editor
        return asList(new FeatureType(TYPE_ANY_OBJECT,
                "KB: Concept/Instance/Property (multi-valued)", featureSupportId));
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), aFeature.getDescription(), CAS.TYPE_NAME_STRING_ARRAY);
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        aFeature.setMode(MultiValueMode.ARRAY);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new MultiValueConceptFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public MultiValueConceptFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        MultiValueConceptFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(MultiValueConceptFeatureTraits.class,
                    aFeature.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new MultiValueConceptFeatureTraits();
        }

        // If there is no scope set in the trait, see if once can be extracted from the legacy
        // location which is the feature type.
        if (traits.getScope() == null && !TYPE_ANY_OBJECT.equals(aFeature.getType())) {
            traits.setScope(aFeature.getType().substring(PREFIX.length()));
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, MultiValueConceptFeatureTraits aTraits)
    {
        // Update the feature type with the scope
        if (aTraits.getScope() != null) {
            aFeature.setType(PREFIX + aTraits.getScope());
        }
        else {
            aFeature.setType(TYPE_ANY_OBJECT);
        }

        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            LOG.error("Unable to write traits", e);
        }
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;
        FeatureEditor editor;

        switch (feature.getMultiValueMode()) {
        case NONE:
            throw unsupportedMultiValueModeException(feature);
        case ARRAY:
            if (feature.getType().startsWith(PREFIX)) {
                editor = new MultiValueConceptFeatureEditor(aId, aOwner, aFeatureStateModel,
                        aStateModel, aHandler);
            }
            else {
                throw unsupportedMultiValueModeException(feature);
            }
            break;
        default:
            throw unsupportedMultiValueModeException(feature);
        }

        return editor;
    }

    @Override
    public List<String> unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof String) {
            return asList((String) aValue);
        }

        // Normally, we get KBHandles back from the feature editors
        if (aValue instanceof List) {
            return ((List<KBHandle>) aValue).stream() //
                    .map(item -> item.getIdentifier()) //
                    .collect(toList());
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

        if (aValue instanceof List) {
            ArrayList<KBHandle> wrapped = new ArrayList<>();

            for (Object item : (List<?>) aValue) {
                if (item instanceof KBHandle) {
                    wrapped.add((KBHandle) aValue);
                    continue;
                }

                if (item instanceof String) {
                    String identifier = (String) item;
                    MultiValueConceptFeatureTraits traits = readTraits(aFeature);
                    KBHandle chbk = labelCache.get(aFeature, traits.getRepositoryId(), identifier);
                    wrapped.add(new KBHandle(chbk.getIdentifier(), chbk.getUiLabel(),
                            chbk.getDescription(), chbk.getLanguage()));
                    continue;
                }

                throw new IllegalArgumentException("Unable to handle list item [" + item
                        + "] of type [" + item.getClass() + "]");
            }

            return wrapped;
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

        FeatureStructure fs = getFS(aCas, aFeature, aAddress);
        List<String> values = unwrapFeatureValue(aFeature, fs.getCAS(), aValue);
        if (values == null) {
            FSUtil.setFeature(fs, aFeature.getName(), (Collection<String>) null);
            return;
        }

        // Create a new array if size differs otherwise re-use existing one
        StringArrayFS array = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);
        if (array == null || (array.size() != values.size())) {
            array = fs.getCAS().createStringArrayFS(values.size());
        }

        // Fill in links
        array.copyFromArray(values.toArray(new String[values.size()]), 0, 0, values.size());

        fs.setFeatureValue(fs.getType().getFeatureByBaseName(aFeature.getName()), array);
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aIdentifier)
    {
        if (aIdentifier == null) {
            return null;
        }

        MultiValueConceptFeatureTraits traits = readTraits(aFeature);
        return labelCache.get(aFeature, traits.getRepositoryId(), aIdentifier).getUiLabel();
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        Feature labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());

        if (labelFeature == null) {
            return null;
        }

        List<KBHandle> handles = getFeatureValue(aFeature, aFs);
        if (handles == null || handles.isEmpty()) {
            return null;
        }

        return handles.stream() //
                .map(KBHandle::getUiLabel) //
                .collect(joining(", "));
    }

    @Override
    public List<VLazyDetailQuery> getLazyDetails(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        Feature labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());

        if (labelFeature == null) {
            return Collections.emptyList();
        }

        List<KBHandle> handles = getFeatureValue(aFeature, aFs);
        return handles.stream() //
                .flatMap(h -> getLazyDetails(aFeature, h.getIdentifier()).stream()) //
                .collect(toList());
    }

    @Override
    public List<VLazyDetailQuery> getLazyDetails(AnnotationFeature aFeature, String aIdentifier)
    {
        if (StringUtils.isEmpty(aIdentifier)) {
            return Collections.emptyList();
        }

        return asList(new VLazyDetailQuery(aFeature.getName(), aIdentifier));
    }

    @Override
    public List<VLazyDetailResult> renderLazyDetails(AnnotationFeature aFeature, String aQuery)
    {
        List<VLazyDetailResult> result = new ArrayList<>();

        MultiValueConceptFeatureTraits traits = readTraits(aFeature);
        KBHandle handle = labelCache.get(aFeature, traits.getRepositoryId(), aQuery);

        result.add(new VLazyDetailResult("Label", handle.getUiLabel()));

        if (isNotBlank(handle.getDescription())) {
            result.add(new VLazyDetailResult("Description", handle.getDescription()));
        }

        return result;
    }
}
