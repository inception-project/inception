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
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
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
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
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

        var traits = readTraits(aFeature);
        traits.getAllowedValueType();

        var uiName = "KB: " + traits.getAllowedValueType();
        if (!TYPE_ANY_OBJECT.equals(aFeature.getType())) {
            uiName += " (" + aFeature.getType().substring(PREFIX.length()) + ")";
        }

        return Optional.of(new FeatureType(aFeature.getType(), uiName, featureSupportId));
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
    public boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        if (aFeature.isRequired()) {
            return isNotBlank(FSUtil.getFeature(aFS, aFeature.getName(), String.class));
        }

        return true;
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aIdentifier)
    {
        if (aIdentifier == null) {
            return null;
        }

        var traits = readTraits(aFeature);
        return getConceptHandle(aFeature, aIdentifier, traits).getUiLabel();
    }

    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return null;
    }

    public KBHandle getConceptHandle(AnnotationFeature aFeature, String aIdentifier,
            ConceptFeatureTraits traits)
    {
        return labelCache.get(aFeature, traits.getRepositoryId(), aIdentifier);
    }

    @SuppressWarnings("unchecked")
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
            var identifier = (String) aValue;
            var traits = readTraits(aFeature);
            var chbk = getConceptHandle(aFeature, identifier, traits);
            // Clone the cached original so we can override the KB
            var clone = new KBHandle(chbk);
            clone.setKB(chbk.getKB());
            return clone;
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
        var feature = aFeatureStateModel.getObject().feature;

        switch (feature.getMultiValueMode()) {
        case NONE:
            if (feature.getType().startsWith(PREFIX)) {
                return new ConceptFeatureEditor(aId, aOwner, aFeatureStateModel, aStateModel,
                        aHandler);
            }
            else {
                throw unsupportedMultiValueModeException(feature);
            }
        case ARRAY: // fall-through
        default:
            throw unsupportedMultiValueModeException(feature);
        }
    }

    @Override
    public ConceptFeatureTraits createDefaultTraits()
    {
        return new ConceptFeatureTraits();
    }

    @Override
    public ConceptFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        var traits = FeatureSupport.super.readTraits(aFeature);

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
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        if (aValue instanceof KBHandle handle) {
            var result = new VLazyDetailGroup(handle.getIdentifier());
            result.addDetail(new VLazyDetail("Label", handle.getUiLabel()));

            if (isNotBlank(handle.getDescription())) {
                result.addDetail(new VLazyDetail("Description", handle.getDescription()));
            }

            return asList(result);
        }

        return emptyList();
    }

    @Override
    public boolean suppressAutoFocus(AnnotationFeature aFeature)
    {
        var traits = readTraits(aFeature);
        return !traits.getKeyBindings().isEmpty();
    }
}
