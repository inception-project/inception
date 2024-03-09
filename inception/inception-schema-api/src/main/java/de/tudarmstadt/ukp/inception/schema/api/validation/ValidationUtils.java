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
package de.tudarmstadt.ukp.inception.schema.api.validation;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class ValidationUtils
{
    public static boolean isRequiredFeatureMissing(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        if (!aFeature.isRequired()) {
            return false;
        }

        if (TYPE_NAME_STRING.equals(aFeature.getType()) || aFeature.isVirtualFeature()) {
            // Only string features can have null values and be required
            return isBlank(FSUtil.getFeature(aFS, aFeature.getName(), String.class));
        }

        if (CAS.TYPE_NAME_STRING_ARRAY.equals(aFeature.getType())) {
            var value = FSUtil.getFeature(aFS, aFeature.getName(), List.class);
            return value == null || value.isEmpty();
        }

        return false;
    }
}
