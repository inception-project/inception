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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * Class for getting list of PossibleValues after evaluating context and applicable rules.
 */
public class ConstraintsEvaluator
    implements Evaluator
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public List<PossibleValue> generatePossibleValues(ParsedConstraints aConstraints,
            FeatureStructure aContext, AnnotationFeature aFeature)
        throws UIMAException
    {
        var restrictionFeaturePath = getRestrictionPathForFeature(aFeature);

        return generatePossibleValues(aConstraints, aContext, restrictionFeaturePath);
    }

    List<PossibleValue> generatePossibleValues(ParsedConstraints aConstraints,
            FeatureStructure aContext, String aFeature)
        throws UIMAException
    {
        if (!aConstraints.isPathUsedInAnyRestriction(aContext, aFeature)) {
            return Collections.emptyList();
        }

        var possibleValues = new ArrayList<PossibleValue>();

        var scope = getScope(aConstraints, aContext);
        for (var rule : scope.getRules()) {
            if (!allRuleConditionsMatch(aConstraints, rule, aContext)) {
                continue;
            }

            for (var res : rule.getRestrictions()) {
                if (aFeature.equals(res.getPath())) {
                    var pv = new PossibleValue(res.getValue(), res.isFlagImportant());
                    possibleValues.add(pv);
                }
            }
        }

        return possibleValues;
    }

    public boolean anyRuleAffectingFeatureMatchesAllConditions(ParsedConstraints aConstraints,
            FeatureStructure aContext, AnnotationFeature aFeature)
    {
        var feature = aFeature.getName();

        if (!aConstraints.isPathUsedInAnyRestriction(aContext, feature)) {
            return false;
        }

        var scope = getScope(aConstraints, aContext);
        if (scope.getRules().isEmpty()) {
            return false;
        }

        for (var rule : scope.getRules()) {
            if (allRuleConditionsMatch(aConstraints, rule, aContext)) {
                if (anyRestrictionAffectsFeature(aConstraints, rule, aFeature)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isValidFeatureValue(ParsedConstraints aConstraints, FeatureStructure aContext,
            AnnotationFeature aFeature)
    {
        var feature = aFeature.getName();

        if (!aConstraints.isPathUsedInAnyRestriction(aContext, feature)) {
            return true;
        }

        var scope = getScope(aConstraints, aContext);
        if (scope.getRules().isEmpty()) {
            return true;
        }

        var actualFeatureValue = FSUtil.getFeature(aContext, feature, String.class);

        for (var rule : scope.getRules()) {
            if (!allRuleConditionsMatch(aConstraints, rule, aContext)) {
                continue;
            }

            for (var res : rule.getRestrictions()) {
                if (res.getPath().equals(feature) && res.getValue().equals(actualFeatureValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isHiddenConditionalFeature(ParsedConstraints aConstraints,
            FeatureStructure aContext, AnnotationFeature aFeature)
    {
        if (!aFeature.isHideUnconstraintFeature()) {
            return false;
        }

        if (aConstraints == null) {
            return false;
        }

        if (!isPathUsedInAnyRestriction(aConstraints, aContext, aFeature)) {
            return false;
        }

        return !anyRuleAffectingFeatureMatchesAllConditions(aConstraints, aContext, aFeature);
    }

    @Override
    public boolean isPathUsedInAnyRestriction(ParsedConstraints aConstraints,
            FeatureStructure aContext, AnnotationFeature aFeature)
    {
        var restrictionFeaturePath = getRestrictionPathForFeature(aFeature);

        return aConstraints.isPathUsedInAnyRestriction(aContext, restrictionFeaturePath);
    }

    private Scope getScope(ParsedConstraints aConstraints, FeatureStructure aContext)
    {
        var shortTypeName = aConstraints.getShortName(aContext.getType().getName());
        if (shortTypeName == null) {
            return null;
        }

        return aConstraints.getScopeByName(shortTypeName);
    }

    private boolean anyRestrictionAffectsFeature(ParsedConstraints aConstraints, Rule aRule,
            AnnotationFeature aFeature)
    {
        var restrictionFeaturePath = getRestrictionPathForFeature(aFeature);

        return aRule.getRestrictions().stream()
                .anyMatch(restriction -> restrictionFeaturePath.equals(restriction.getPath()));
    }

    private boolean allRuleConditionsMatch(ParsedConstraints aConstraints, Rule aRule,
            FeatureStructure aContext)
    {
        return aRule.getConditions().stream()
                .allMatch(condition -> conditionMatches(aConstraints, aContext, condition));
    }

    private boolean conditionMatches(ParsedConstraints aConstraints, FeatureStructure aContext,
            Condition aCondition)
    {
        var values = getValue(aConstraints, aContext, aCondition.getPath());
        var result = aCondition.matchesAny(values);

        if (LOG.isTraceEnabled()) {
            LOG.trace("comparing [{}]/@{}[{} == {}] -> {}", aContext.getType().getName(),
                    aCondition.getPath(), aCondition.getValue(), values, result);
        }

        return result;
    }

    private List<String> getValue(ParsedConstraints aConstraints, FeatureStructure aContext,
            String aPath)
    {
        String head, tail;

        if (aPath.contains(".")) {
            // Separate first part of path to be processed.
            head = aPath.substring(0, aPath.indexOf("."));
            tail = aPath.substring(aPath.indexOf(".") + 1);
        }
        else {
            head = aPath;
            tail = "";
        }

        if (head.startsWith("@")) {
            return crossLayerStep(aConstraints, aContext, head, tail);
        }
        else if (head.endsWith("()")) {
            return functionStep(aConstraints, aContext, aPath, head, tail);
        }
        else if (StringUtils.isNotEmpty(tail)) {
            return featureStep(aConstraints, aContext, head, tail);
        }
        else {
            var feature = aContext.getType().getFeatureByBaseName(head);
            if (feature == null) {
                throw new IllegalStateException("Feature [" + head + "] does not exist on type ["
                        + aContext.getType().getName() + "]");
            }

            return asList(aContext.getFeatureValueAsString(feature));
        }
    }

    private List<String> featureStep(ParsedConstraints aConstraints, FeatureStructure aContext,
            String head, String tail)
    {
        var feature = aContext.getType().getFeatureByBaseName(head);
        if (feature == null) {
            throw new IllegalStateException("Feature [" + head + "] does not exist on type ["
                    + aContext.getType().getName() + "]");
        }

        if (FSUtil.isMultiValuedFeature(aContext, head)) {
            var values = new ArrayList<String>();
            for (var fs : FSUtil.getFeature(aContext, head, FeatureStructure[].class)) {
                values.addAll(getValue(aConstraints, fs, tail));
            }

            return values;
        }

        return getValue(aConstraints, aContext.getFeatureValue(feature), tail);
    }

    private List<String> functionStep(ParsedConstraints aConstraints, FeatureStructure aContext,
            String aPath, String head, String tail)
    {
        if (StringUtils.isNotEmpty(tail)) {
            throw new IllegalStateException("No additional steps possible after function");
        }

        if ("text()".equals(head)) {
            if (!(aContext instanceof AnnotationFS)) {
                throw new IllegalStateException("Cannot use [text()] on non-annotations");
            }

            return asList(((AnnotationFS) aContext).getCoveredText());
        }
        else {
            throw new IllegalStateException("Unknown path function [" + aPath + "]");
        }
    }

    private List<String> crossLayerStep(ParsedConstraints aConstraints, FeatureStructure aContext,
            String head, String tail)
    {
        var typename = aConstraints.getImports().get(head.substring(1));
        var type = aContext.getCAS().getTypeSystem().getType(typename);
        var ctxAnnFs = (AnnotationFS) aContext;

        var values = new ArrayList<String>();
        var cas = aContext.getCAS();
        for (var fs : cas.<Annotation> select(type).at(ctxAnnFs.getBegin(), ctxAnnFs.getEnd())) {
            values.addAll(getValue(aConstraints, fs, tail));
        }
        return values;
    }

    private String getRestrictionPathForFeature(AnnotationFeature aFeature)
    {
        // Add values from rules
        String restrictionFeaturePath;
        switch (aFeature.getLinkMode()) {
        case WITH_ROLE:
            restrictionFeaturePath = aFeature.getName() + "."
                    + aFeature.getLinkTypeRoleFeatureName();
            break;
        case NONE:
            restrictionFeaturePath = aFeature.getName();
            break;
        default:
            throw new IllegalArgumentException("Unsupported link mode [" + aFeature.getLinkMode()
                    + "] on feature [" + aFeature.getName() + "]");
        }
        return restrictionFeaturePath;
    }
}
