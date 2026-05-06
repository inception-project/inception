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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureExtractorSupport;

public class StringFeatureExtractorSupport
    implements FeatureExtractorSupport
{
    @Override
    public boolean accepts(AnnotationFeature aContext)
    {
        return TYPE_NAME_STRING.equals(aContext.getType()) && aContext.getMultiValueMode() == NONE;
    }

    @Override
    public Extractor<?, ?> createExtractor(AnnotationFeature aFeature)
    {
        return new StringFeatureExtractor(aFeature);
    }

    @Override
    public String renderName(AnnotationFeature aFeature)
    {
        return aFeature.getLayer().getUiName() + " :: " + aFeature.getUiName();
    }
}
