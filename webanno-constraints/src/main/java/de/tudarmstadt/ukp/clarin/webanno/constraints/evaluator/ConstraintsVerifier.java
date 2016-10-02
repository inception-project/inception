/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;

public class ConstraintsVerifier
    implements Verifiable
{

    @Override
    public boolean verify(FeatureStructure featureStructure, ParsedConstraints parsedConstraints)
    {

        boolean isOk = false;
        Type type = featureStructure.getType();
        for (Feature feature : type.getFeatures()) {
            if (feature.getRange().isPrimitive()) {
                String scopeName = featureStructure.getFeatureValueAsString(feature);
                List<Rule> rules = parsedConstraints.getScopeByName(scopeName).getRules();

                // Check if all the feature values are ok according to the
                // rules;
            }
            else {
                // Here some recursion would be in order
            }
        }
        return isOk;
    }

}
