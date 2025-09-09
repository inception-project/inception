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
package de.tudarmstadt.ukp.inception.annotation.layer.span.pivot;

import static de.tudarmstadt.ukp.inception.pivot.api.extractor.AnnotationExtractor.TEXT;
import static java.util.Arrays.asList;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerExtractorSupport;

public class SpanCoveredTextExtractorSupport
    implements LayerExtractorSupport
{
    @Override
    public String renderName(AnnotationLayer aLayer)
    {
        return aLayer.getUiName() + " :: " + TEXT;
    }

    @Override
    public boolean accepts(AnnotationLayer aContext)
    {
        if (aContext == null) {
            return false;
        }

        return asList(SpanLayerSupport.TYPE).contains(aContext.getType());
    }

    @Override
    public Extractor<?, ?> createExtractor(AnnotationLayer aLayer)
    {
        return new SpanCoveredTextExtractor(aLayer);
    }
}
