/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class PrimitiveUimaFeatureSupport
    implements FeatureSupport
{
    private final static List<String> PRIMITIVE_TYPES = asList(CAS.TYPE_NAME_STRING,
            CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_BOOLEAN);

    @Override
    public List<String> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return PRIMITIVE_TYPES;
    }
}
