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
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
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
 * {@link KnowledgeBaseServiceUIAutoConfiguration#conceptFeatureSupport}.
 * </p>
 */
public class ConceptFeatureSupport
    implements FeatureSupport<ConceptFeatureTraits>
{
    public static final String PREFIX = "kb:";

    public static final String ANY_OBJECT = "<ANY>";
    public static final String TYPE_ANY_OBJECT = PREFIX + ANY_OBJECT;

    private static final Logger LOG = LoggerFactory.getLogger(ConceptFeatureSupport.class);

    private final ConceptLabelCache labelCache;

    private String featureSupportId;

    @Autowired
    public ConceptFeatureSupport(ConceptLabelCache aLabelCache)
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
    public Optional<FeatureType> getFeatureType(AnnotationFeature aFeature)
    {
        if (!aFeature.getType().startsWith(PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(new FeatureType(aFeature.getType(),
                aFeature.getType().substring(PREFIX.length()), featureSupportId));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        // We just start with no specific scope at all (ANY) and let the user refine this via
        // the traits editor
        return asList(new FeatureType(TYPE_ANY_OBJECT, "KB: Concept/Instance/Property",
                featureSupportId));
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            return aFeature.getType().startsWith(PREFIX);
        case ARRAY: // fall-through
        default:
            return false;
        }
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aIdentifier)
    {
        if (aIdentifier == null) {
            return null;
        }

        ConceptFeatureTraits traits = readTraits(aFeature);
        return labelCache.get(aFeature, traits.getRepositoryId(), aIdentifier).getUiLabel();
    }

    @Override
    public String unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        // When used in a recommendation context, we might get the concept identifier as a string
        // value.
        if (aValue == null || aValue instanceof String) {
            return (String) aValue;
        }

        // Normally, we get KBHandles back from the feature editors
        if (aValue instanceof KBHandle) {
            return ((KBHandle) aValue).getIdentifier();
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public KBHandle wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null || aValue instanceof KBHandle) {
            return (KBHandle) aValue;
        }

        if (aValue instanceof String) {
            String identifier = (String) aValue;
            String label = renderFeatureValue(aFeature, identifier);
            ConceptFeatureTraits traits = readTraits(aFeature);
            String description = labelCache.get(aFeature, traits.getRepositoryId(), identifier)
                    .getDescription();

            return new KBHandle(identifier, label, description);
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new ConceptFeatureTraitsEditor(aId, this, aFeatureModel);
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
            if (feature.getType().startsWith(PREFIX)) {
                editor = new ConceptFeatureEditor(aId, aOwner, aFeatureStateModel, aStateModel,
                        aHandler);
            }
            else {
                throw unsupportedMultiValueModeException(feature);
            }
            break;
        case ARRAY: // fall-through
        default:
            throw unsupportedMultiValueModeException(feature);
        }

        return editor;
    }

    @Override
    public ConceptFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        ConceptFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(ConceptFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new ConceptFeatureTraits();
        }

        // If there is no scope set in the trait, see if once can be extracted from the legacy
        // location which is the feature type.
        if (traits.getScope() == null && !TYPE_ANY_OBJECT.equals(aFeature.getType())) {
            traits.setScope(aFeature.getType().substring(PREFIX.length()));
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, ConceptFeatureTraits aTraits)
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
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_STRING);
    }

    @Override
    public List<VLazyDetailQuery> getLazyDetails(AnnotationFeature aFeature, String aLabel)
    {
        if (StringUtils.isEmpty(aLabel)) {
            return Collections.emptyList();
        }

        return asList(new VLazyDetailQuery(aFeature.getName(), aLabel));
    }

    @Override
    public List<VLazyDetailResult> renderLazyDetails(AnnotationFeature aFeature, String aQuery)
    {
        List<VLazyDetailResult> result = new ArrayList<>();

        ConceptFeatureTraits traits = readTraits(aFeature);
        KBHandle handle = labelCache.get(aFeature, traits.getRepositoryId(), aQuery);

        result.add(new VLazyDetailResult("Label", handle.getUiLabel()));

        if (isNotBlank(handle.getDescription())) {
            result.add(new VLazyDetailResult("Description", handle.getDescription()));
        }

        return result;
    }

    @Override
    public boolean suppressAutoFocus(AnnotationFeature aFeature)
    {
        ConceptFeatureTraits traits = readTraits(aFeature);
        return !traits.getKeyBindings().isEmpty();
    }
}
