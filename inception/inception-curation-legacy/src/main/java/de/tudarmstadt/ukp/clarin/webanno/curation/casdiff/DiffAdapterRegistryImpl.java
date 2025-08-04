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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.NONE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.EXCLUDE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.curation.DocumentMetadataDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapter;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.api.DiffSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class DiffAdapterRegistryImpl
    implements DiffAdapterRegistry
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService schemaService;
    private final DiffSupportRegistry diffSupportRegistry;

    public DiffAdapterRegistryImpl(AnnotationSchemaService aSchemaService,
            DiffSupportRegistry aDiffSupportRegistry)
    {
        schemaService = aSchemaService;
        diffSupportRegistry = aDiffSupportRegistry;
    }

    @Override
    public List<DiffAdapter> getDiffAdapters(Collection<AnnotationLayer> aLayers)
    {
        if (aLayers.isEmpty()) {
            return emptyList();
        }

        var project = aLayers.iterator().next().getProject();

        var featuresByLayer = schemaService.listSupportedFeatures(project).stream() //
                .collect(groupingBy(AnnotationFeature::getLayer));

        var adapters = new ArrayList<DiffAdapter>();
        nextLayer: for (var layer : aLayers) {
            if (!layer.isEnabled()) {
                continue nextLayer;
            }

            var featuresToCompare = collectFeaturesToCompare(featuresByLayer, layer);

            DiffAdapter adapter;
            switch (layer.getType()) {
            case SpanLayerSupport.TYPE: // Fall through
            case RelationLayerSupport.TYPE: {
                var diffSupport = diffSupportRegistry.getExtension(layer).get();
                adapter = diffSupport.getAdapter(layer, featuresToCompare);
                break;
            }
            case DocumentMetadataLayerSupport.TYPE: {
                adapter = new DocumentMetadataDiffAdapter(layer.getName(), featuresToCompare);
                break;
            }
            default:
                LOG.debug("Layer type [{}] not supported - ignoring", layer.getType());
                continue nextLayer;
            }

            adapters.add(adapter);

            nextFeature: for (var f : featuresByLayer.getOrDefault(layer, emptyList())) {
                if (!f.isEnabled()) {
                    continue nextFeature;
                }

                switch (f.getLinkMode()) {
                case NONE:
                    // Nothing to do here
                    break;
                case SIMPLE:
                    adapter.addLinkFeature(f.getName(), f.getLinkTypeRoleFeatureName(),
                            f.getLinkTypeTargetFeatureName(), ONE_TARGET_MULTIPLE_ROLES, EXCLUDE);
                    break;
                case WITH_ROLE: {
                    var typeAdpt = schemaService.getAdapter(layer);
                    var traits = typeAdpt.getFeatureTraits(f, LinkFeatureTraits.class)
                            .orElse(new LinkFeatureTraits());
                    adapter.addLinkFeature(f.getName(), f.getLinkTypeRoleFeatureName(),
                            f.getLinkTypeTargetFeatureName(), traits.getMultiplicityMode(),
                            traits.getDiffMode());
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown link mode [" + f.getLinkMode() + "]");
                }

                featuresToCompare.add(f.getName());
            }
        }

        // If the token/sentence layer is not editable, we do not offer curation of the tokens.
        // Instead the tokens are obtained from a random template CAS when initializing the CAS - we
        // assume here that the tokens have never been modified.
        if (!schemaService.isSentenceLayerEditable(project)) {
            adapters.removeIf(adapter -> Sentence._TypeName.equals(adapter.getType()));
        }

        if (!schemaService.isTokenLayerEditable(project)) {
            adapters.removeIf(adapter -> Token._TypeName.equals(adapter.getType()));
        }

        return adapters;
    }

    private static Set<String> collectFeaturesToCompare(
            Map<AnnotationLayer, List<AnnotationFeature>> featuresByLayer, AnnotationLayer layer)
    {
        var features = new LinkedHashSet<String>();

        nextFeature: for (var f : featuresByLayer.getOrDefault(layer, emptyList())) {
            if (!f.isEnabled() || !f.isCuratable()) {
                continue nextFeature;
            }

            // Link features are treated separately from primitive label features
            if (!NONE.equals(f.getLinkMode())) {
                continue nextFeature;
            }

            features.add(f.getName());
        }

        return features;
    }
}
