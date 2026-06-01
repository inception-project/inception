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
package de.tudarmstadt.ukp.inception.pivot.extractor;

import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBindingResolutionContext;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.GeneralBinding;
import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;

public class TypeExtractorSupport
    implements ExtractorSupport
{
    @Override
    public String getId()
    {
        return "type";
    }

    @Override
    public ExtractorBinding bindingFromDef(ExtractorDef aDef,
            ExtractorBindingResolutionContext aContext)
    {
        return new GeneralBinding();
    }

    @Override
    public String renderLabel(ExtractorBinding aBinding)
    {
        return "<type>";
    }

    @Override
    public boolean accepts(ExtractorBinding aBinding)
    {
        return aBinding instanceof GeneralBinding;
    }

    @Override
    public Extractor<?, ?> createExtractor(ExtractorBinding aBinding)
    {
        return new TypeExtractor(null);
    }
}
