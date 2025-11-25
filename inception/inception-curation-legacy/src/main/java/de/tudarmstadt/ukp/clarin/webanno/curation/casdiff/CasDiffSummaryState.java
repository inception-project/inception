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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;

import java.util.HashSet;

import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationPosition;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;

/**
 * An enumeration to differentiate sentences in a document with different colors so as to easily
 * identify
 *
 */
public enum CasDiffSummaryState
{
    /**
     * No conflicts of annotation in this sentence, no color - null- white
     */
    AGREE("agree"),

    /**
     * Stacked annotations
     */
    STACKED("stacked"),

    /**
     * Incomplete annotations
     */
    INCOMPLETE("incomplete"),

    /**
     * Conflicts of annotation found in this sentence, mark background in red
     */
    DISAGREE("disagree"),

    /**
     * Confirmed annotation.
     */
    CURATED("curated");

    private String cssClass;

    CasDiffSummaryState(String aCssClass)
    {
        cssClass = aCssClass;
    }

    public String getCssClass()
    {
        return cssClass;
    }

    private static boolean isTextLevel(ConfigurationSet aCfg)
    {
        var position = aCfg.getPosition();
        return position instanceof SpanPosition || position instanceof RelationPosition;
    }

    public static CasDiffSummaryState calculateState(DiffResult aDiff)
    {
        var differingSets = aDiff.getDifferingConfigurationSetsWithExceptions(CURATION_USER)
                .values().stream() //
                .filter(CasDiffSummaryState::isTextLevel) //
                .toList();

        // CURATED:
        // The curation user participates in every configuration set
        var textLevelConfigurations = aDiff.getConfigurationSets().stream() //
                .filter(CasDiffSummaryState::isTextLevel) //
                .toList();
        var allCurated = textLevelConfigurations.stream() //
                .allMatch(set -> set.getCasGroupIds().contains(CURATION_USER));
        if (!textLevelConfigurations.isEmpty() && allCurated) {
            return CURATED;
        }

        // AGREE:
        // - there are no differences between the annotators
        // - the annotations are complete
        var incompleteConfigurationSetsWithExceptions = aDiff
                .getIncompleteConfigurationSetsWithExceptions(CURATION_USER).values().stream() //
                .filter(CasDiffSummaryState::isTextLevel) //
                .toList();
        if (differingSets.isEmpty() && incompleteConfigurationSetsWithExceptions.isEmpty()) {
            return AGREE;
        }

        // Is this confSet a diff due to stacked annotations (with same configuration)?
        var stackedDiff = false;
        stackedDiffSet: for (var set : differingSets) {
            for (var user : set.getCasGroupIds()) {
                if (set.getConfigurations(user).size() > 1) {
                    stackedDiff = true;
                    break stackedDiffSet;
                }
            }
        }

        if (stackedDiff) {
            return STACKED;
        }

        var usersExceptCurator = new HashSet<>(aDiff.getCasGroupIds());
        usersExceptCurator.remove(CURATION_USER);
        var incompleteSets = aDiff.getIncompleteConfigurationSets().values().stream() //
                .filter(CasDiffSummaryState::isTextLevel) //
                .toList();
        for (var set : incompleteSets) {
            if (!set.getCasGroupIds().containsAll(usersExceptCurator)) {
                return INCOMPLETE;
            }
        }

        return DISAGREE;
    }
}
