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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.pivot;

import java.util.function.Function;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerBinding;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.NoResultException;

/**
 * Extracts the covered text or the offset range of one endpoint (source or target) of a relation.
 * The endpoint to extract and the representation (text vs. range) are passed in, so a single
 * support covers all four source/target × text/range combinations.
 */
public class RelationEndpointExtractorSupport
    implements ExtractorSupport
{
    private final AnnotationSchemaService schemaService;
    private final String id;
    private final Function<RelationAdapter, String> endpointFeatureName;
    private final Representation representation;

    public RelationEndpointExtractorSupport(AnnotationSchemaService aSchemaService, String aId,
            Function<RelationAdapter, String> aEndpointFeatureName, Representation aRepresentation)
    {
        schemaService = aSchemaService;
        id = aId;
        endpointFeatureName = aEndpointFeatureName;
        representation = aRepresentation;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public boolean accepts(ExtractorBinding aBinding)
    {
        return aBinding instanceof LayerBinding lb
                && RelationLayerSupport.TYPE.equals(lb.layer().getType());
    }

    @Override
    public Extractor<?, ?> createExtractor(ExtractorBinding aBinding)
    {
        var layer = ((LayerBinding) aBinding).layer();
        var adapter = (RelationAdapter) schemaService.getAdapter(layer);
        var featureName = endpointFeatureName.apply(adapter);

        return representation.createExtractor(layer, featureName, schemaService);
    }

    @Override
    public String renderLabel(ExtractorBinding aBinding)
    {
        var layer = ((LayerBinding) aBinding).layer();
        var adapter = (RelationAdapter) schemaService.getAdapter(layer);
        var featureName = endpointFeatureName.apply(adapter);

        try {
            var feature = schemaService.getFeature(featureName, layer);
            return feature.getUiName() + " " + representation.label();
        }
        catch (NoResultException e) {
            return featureName + " " + representation.label();
        }
    }

    public enum Representation
    {
        TEXT("<text>")
        {
            @Override
            Extractor<?, ?> createExtractor(AnnotationLayer aLayer, String aFeatureName,
                    AnnotationSchemaService aSchemaService)
            {
                return new RelationEndpointTextExtractor(aLayer, aFeatureName);
            }
        },
        RANGE("<range>")
        {
            @Override
            Extractor<?, ?> createExtractor(AnnotationLayer aLayer, String aFeatureName,
                    AnnotationSchemaService aSchemaService)
            {
                var feature = aSchemaService.getFeature(aFeatureName, aLayer);
                return new RelationEndpointRangeExtractor(feature);
            }
        };

        private final String label;

        Representation(String aLabel)
        {
            label = aLabel;
        }

        String label()
        {
            return label;
        }

        abstract Extractor<?, ?> createExtractor(AnnotationLayer aLayer, String aFeatureName,
                AnnotationSchemaService aSchemaService);
    }
}
