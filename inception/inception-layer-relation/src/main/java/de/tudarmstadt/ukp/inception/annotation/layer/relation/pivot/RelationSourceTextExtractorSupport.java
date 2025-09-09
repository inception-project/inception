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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerExtractorSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.NoResultException;

public class RelationSourceTextExtractorSupport
    implements LayerExtractorSupport
{
    private AnnotationSchemaService schemaService;

    public RelationSourceTextExtractorSupport(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public boolean accepts(AnnotationLayer aContext)
    {
        return aContext != null && RelationLayerSupport.TYPE.equals(aContext.getType());
    }

    @Override
    public Extractor<?, ?> createExtractor(AnnotationLayer aLayer)
    {
        var adapter = (RelationAdapter) schemaService.getAdapter(aLayer);

        return new RelationEndpointTextExtractor(aLayer, adapter.getSourceFeatureName());
    }

    @Override
    public String renderName(AnnotationLayer aLayer)
    {
        var adapter = (RelationAdapter) schemaService.getAdapter(aLayer);
        try {
            var feature = schemaService.getFeature(adapter.getSourceFeatureName(), aLayer);
            return aLayer.getUiName() + " :: " + feature.getUiName() + " <text>";
        }
        catch (NoResultException e) {
            return aLayer.getUiName() + " :: " + adapter.getSourceFeatureName() + " <text>";
        }
    }
}
