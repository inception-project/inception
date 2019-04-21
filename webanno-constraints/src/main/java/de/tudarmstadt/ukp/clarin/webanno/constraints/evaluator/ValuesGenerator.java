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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

/**
 * Class for getting list of PossibleValues after evaluating context and applicable rules.
 * 
 *
 */
public class ValuesGenerator
    implements Evaluator
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    Map<String, String> imports = null;
    private Scope scope;
    private String shortTypeName;

    @Override
    public List<PossibleValue> generatePossibleValues(FeatureStructure aContext, String aFeature,
            ParsedConstraints parsedConstraints)
        throws UIMAException
    {
        imports = parsedConstraints.getImports();
        List<PossibleValue> possibleValues = new ArrayList<>();
        //If there are no rules for the FS, don't execute further
        //Enabling the second option might take too much time for rules to execute.
//        if(!areThereRulesFor(aContext, parsedConstraints)){
//        if(!isThisAffectedByConstraintRules(aContext, aFeature, parsedConstraints)){
        if (!parsedConstraints.areThereRules(aContext.getType().getName(), aFeature)) {
            return possibleValues;
        }
       
//        String shortTypeName = parsedConstraints.getShortName(aContext.getType().getName());
//        if (shortTypeName == null) {
//            //If no relevant rules are there for a particular type, just return empty list
//            log.error("No import for [" + aContext.getType().getName()
//                    + "] - Imports are: [" + parsedConstraints.getImports() + "]");
//            return possibleValues;
////            throw new IllegalStateException("No import for [" + aContext.getType().getName()
////                    + "] - Imports are: [" + parsedConstraints.getImports() + "]");
//        }
        shortTypeName = parsedConstraints.getShortName(aContext.getType().getName());
        scope = parsedConstraints.getScopeByName(shortTypeName);

      
        for (Rule rule : scope.getRules()) {
            // Check if conditions apply
            if (!ruleTriggers(aContext, rule)) {
                continue;
            }

            // If all conditions apply, collect the possible values
            for (Restriction res : rule.getRestrictions()) {
                if (aFeature.equals(res.getPath())) {
                    PossibleValue pv = new PossibleValue(res.getValue(), res.isFlagImportant());
                    possibleValues.add(pv);
                }
            }
        }

        return possibleValues;
    }

    private boolean ruleTriggers(FeatureStructure aContext, Rule aRule)
        throws UIMAException
    {
        boolean doAllConditionsMatch = false;
        for (Condition condition : aRule.getConditions()) {
            if (conditionMatches(aContext, condition)) {
                doAllConditionsMatch = true;
            }
            else {
                doAllConditionsMatch = false;
                break;
            }
        }
        return doAllConditionsMatch;

    }

    private boolean conditionMatches(FeatureStructure aContext, Condition aCondition)
        throws UIMAException
    {
        ArrayList<String> value = getValue(aContext, aCondition.getPath());
        if (log.isTraceEnabled()) {
            log.trace("comparing [" + aCondition.getValue() + "] to [" + value + "]");
        }
        return aCondition.matches(value);
    }

    private ArrayList<String> getValue(FeatureStructure aContext, String aPath)
        throws UIMAException
    {
        String head, tail;
        if (aPath.contains(".")) {
            head = aPath.substring(0, aPath.indexOf(".")); // Separate first part of path to be
                                                           // processed.
            tail = aPath.substring(aPath.indexOf(".") + 1); // The remaining part
        }
        else {
            head = aPath;
            tail = "";
        }

        List<String> values = new ArrayList<>();

        if (head.startsWith("@")) {
            String typename = imports.get(head.substring(1));
            Type type = aContext.getCAS().getTypeSystem().getType(typename);
            AnnotationFS ctxAnnFs = (AnnotationFS) aContext;
            // List<String> values = new ArrayList<>();
            for (AnnotationFS fs : selectAt(aContext.getCAS(), type, ctxAnnFs.getBegin(),
                    ctxAnnFs.getEnd())) {
                values.addAll(getValue(fs, tail));

            }
            return (ArrayList<String>) values;
        }
        else if (head.endsWith("()")) {
            if (StringUtils.isNotEmpty(tail)) {
                throw new IllegalStateException("No additional steps possible after function");
            }

            if ("text()".equals(head)) {
                if (aContext instanceof AnnotationFS) {
                    values.add(((AnnotationFS) aContext).getCoveredText());
                    return (ArrayList<String>) values;
                }
                else {
                    throw new IllegalStateException("Cannot use [text()] on non-annotations");
                }
            }
            else {
                throw new IllegalStateException("Unknown path function [" + aPath + "]");
            }
        }
        else if (StringUtils.isNotEmpty(tail)) {

            /*
             * Extracting feature and passing FeatureStructure based on that. Shortening the path
             * variable by removing first element in the aPath separated by "." (dot)
             */
            Feature feature = aContext.getType().getFeatureByBaseName(
                    aPath.substring(0, aPath.indexOf(".")));
            if (feature == null) {
                throw new IllegalStateException("Feature [" + aPath + "] does not exist on type ["
                        + aContext.getType().getName() + "]");
            }
            return getValue(aContext.getFeatureValue(feature),
                    aPath.substring(aPath.indexOf(".") + 1));

            // throw new UnsupportedOperationException("Error in rule");
        }
        else {
            Feature feature = aContext.getType().getFeatureByBaseName(aPath);
            if (feature == null) {
                throw new IllegalStateException("Feature [" + aPath + "] does not exist on type ["
                        + aContext.getType().getName() + "]");
            }
            values.add(aContext.getFeatureValueAsString(feature));
            return (ArrayList<String>) values;
        }

    }

    public static List<AnnotationFS> selectAt(CAS aCas, final Type type, int aBegin, int aEnd)
    {
        List<AnnotationFS> covered = CasUtil.selectCovered(aCas, type, aBegin, aEnd);

        // Remove all that do not have the exact same offset
        covered.removeIf(cur -> !(cur.getBegin() == aBegin && cur.getEnd() == aEnd));
        return covered;
    }

//    @Override
//    public boolean areThereRulesFor(FeatureStructure aContext, 
//            ParsedConstraints parsedConstraints)
//    {
//        if(imports==null){
//            imports = parsedConstraints.getImports();
//        }
//        shortTypeName = parsedConstraints.getShortName(aContext.getType().getName());
//        if (shortTypeName == null) {
//          //If no relevant rules are there for a particular type, just return empty list
//            log.error("No import for [" + aContext.getType().getName()
//                    + "] - Imports are: [" + parsedConstraints.getImports() + "]");
//            return false;
//        }
//        //If there's import statement but no scope rules defined for it
//        else if(parsedConstraints.getScopeByName(shortTypeName)==null
//              //If there's scope but no rules defined in it.
//                || parsedConstraints.getScopeByName(shortTypeName).getRules().isEmpty()){
//            log.debug("No rules found in scope [" + shortTypeName + "]");
//            return false;
//        }else{
//            return true;
//        }
//         
//    }

    /**
     *Checks if it is necessary to evaluate rules based on 
        1. whether there are rules for this FeatureStructure and
        2. whether the target is affected by any of the restrictions within rules
     
     */
    @Override
    public boolean isThisAffectedByConstraintRules(FeatureStructure aContext, String aFeature,
            ParsedConstraints parsedConstraints)
    {
        return parsedConstraints.areThereRules(aContext.getType().getName(), aFeature);
//        if (!areThereRulesFor(aContext, parsedConstraints)) {
//            return false;
//        }
//        else {
//            shortTypeName = parsedConstraints.getShortName(aContext.getType().getName());
//            scope = parsedConstraints.getScopeByName(shortTypeName);
//            //Check within every rule if any restriction affects aFeature
//            for (Rule rule : scope.getRules()) {
//                for (Restriction res : rule.getRestrictions()) {
//                    if (aFeature.equals(res.getPath())) {
//                        return true;
//                    }
//                }
//            }
//            return false;
//
//        }
    }
}
    
