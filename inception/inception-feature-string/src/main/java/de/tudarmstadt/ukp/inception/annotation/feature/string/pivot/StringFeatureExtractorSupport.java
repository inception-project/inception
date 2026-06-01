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
package de.tudarmstadt.ukp.inception.annotation.feature.string.pivot;

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.NONE;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBindingResolutionContext;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureBinding;
import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;

public class StringFeatureExtractorSupport
    implements ExtractorSupport
{
    @Override
    public String getId()
    {
        return "stringFeature";
    }

    @Override
    public ExtractorBinding bindingFromDef(ExtractorDef aDef,
            ExtractorBindingResolutionContext aContext)
    {
        var feature = aContext.resolveFeature(aDef.getLayer(), aDef.getFeature());
        return feature != null ? new FeatureBinding(feature) : null;
    }

    @Override
    public boolean accepts(ExtractorBinding aBinding)
    {
        return aBinding instanceof FeatureBinding fb
                && TYPE_NAME_STRING.equals(fb.feature().getType())
                && fb.feature().getMultiValueMode() == NONE;
    }

    @Override
    public Extractor<?, ?> createExtractor(ExtractorBinding aBinding)
    {
        return new StringFeatureExtractor(((FeatureBinding) aBinding).feature());
    }

    @Override
    public String renderLabel(ExtractorBinding aBinding)
    {
        return ((FeatureBinding) aBinding).feature().getUiName();
    }
}
