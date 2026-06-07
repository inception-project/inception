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
package de.tudarmstadt.ukp.inception.annotation.layer.chain.pivot;

import static de.tudarmstadt.ukp.inception.pivot.api.extractor.AnnotationExtractor.RANGE;

import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ContextualizedFS;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerBinding;

public class ChainRangeExtractorSupport
    implements ExtractorSupport
{
    @Override
    public String getId()
    {
        return "chain.range";
    }

    @Override
    public String renderLabel(ExtractorBinding aBinding)
    {
        return RANGE;
    }

    @Override
    public boolean accepts(ExtractorBinding aBinding)
    {
        return aBinding instanceof LayerBinding lb
                && ChainLayerSupport.TYPE.equals(lb.layer().getType());
    }

    @Override
    public Extractor<ContextualizedFS<Annotation>, String> createExtractor(
            ExtractorBinding aBinding)
    {
        return new ChainRangeExtractor<>(((LayerBinding) aBinding).layer());
    }
}
