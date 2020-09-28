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

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;

/***
 * Interface for getting values from rules.
 */
public interface Evaluator
{
    /**
     * @param aFeature
     *            the target feature
     * @param parsedConstraints
     *            the object containing object generated after parsing rules
     * @return list of possible values based on rules
     */
    List<PossibleValue> generatePossibleValues(FeatureStructure aContext, String aFeature,
            ParsedConstraints parsedConstraints)
        throws UIMAException;

    // /**
    // *
    // * @param aContext The feature structure /Scope in the rules
    // * @param parsedConstraints Object containing parsed rules
    // * @return true if there are rules for the feature structure
    // */
    // boolean areThereRulesFor(FeatureStructure aContext, ParsedConstraints parsedConstraints);

    /**
     * @param aContext
     *            The feature structure /Scope in the rules
     * @param aFeature
     *            The affected feature
     * @param parsedConstraints
     *            Object containing parsed rules
     * @return true if features can be affected by this execution
     */
    boolean isThisAffectedByConstraintRules(FeatureStructure aContext, String aFeature,
            ParsedConstraints parsedConstraints);
}
