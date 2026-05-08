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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.curation;

import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.curation.api.DiffSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class RelationDiffSupport
    implements DiffSupport
{
    private final AnnotationSchemaService schemaService;

    public RelationDiffSupport(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public boolean accepts(AnnotationLayer aContext)
    {
        return RelationLayerSupport.TYPE.equals(aContext.getType());
    }

    @Override
    public RelationDiffAdapter getAdapter(AnnotationLayer aLayer, Set<String> featuresToCompare)
    {
        var typeAdpt = (RelationAdapter) schemaService.getAdapter(aLayer);
        return new RelationDiffAdapterImpl(aLayer.getName(), typeAdpt.getSourceFeatureName(),
                typeAdpt.getTargetFeatureName(), featuresToCompare);
    }
}
