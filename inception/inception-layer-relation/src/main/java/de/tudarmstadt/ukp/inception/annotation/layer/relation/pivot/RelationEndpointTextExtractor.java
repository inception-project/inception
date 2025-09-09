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

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.AnnotationExtractor_ImplBase;

public class RelationEndpointTextExtractor
    extends AnnotationExtractor_ImplBase<AnnotationFS, String>
{
    private final String feature;

    public RelationEndpointTextExtractor(AnnotationLayer aLayer, String aFeature)
    {
        super(aLayer);
        feature = aFeature;
    }

    @Override
    public Class<String> getResultType()
    {
        return String.class;
    }

    @Override
    public String extract(AnnotationFS aAnn)
    {
        var f = aAnn.getType().getFeatureByBaseName(feature);
        var a = (AnnotationFS) aAnn.getFeatureValue(f);
        return a != null ? a.getCoveredText() : null;
    }

    @Override
    public String getName()
    {
        return "<text>";
    }
}
