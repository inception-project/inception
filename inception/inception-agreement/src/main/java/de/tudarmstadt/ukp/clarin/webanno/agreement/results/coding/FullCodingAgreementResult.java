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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.FullAgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag;

public class FullCodingAgreementResult
    extends FullAgreementResult_ImplBase<ICodingAnnotationStudy>
{
    private static final long serialVersionUID = -1262324752699430461L;

    protected final DiffResult diff;
    protected final List<ConfigurationSet> allSets;

    /**
     * Whether the measure scores items on which only one rater gave a value. The study may contain
     * such items when incomplete positions are included, yet some measures - e.g. Krippendorff's
     * alpha or Gwet's AC2 - cannot pair a single value with anything and ignore those items. The
     * measure knows, so it declares it here, and the diagnostics then describe the data the score
     * is actually based on.
     */
    private boolean scoringSingleValueItems = true;

    public FullCodingAgreementResult(String aType, String aFeature, DiffResult aDiff,
            ICodingAnnotationStudy aStudy, List<String> aCasGroupIds,
            List<ConfigurationSet> aTaggedConfigurations, boolean aExcludeIncomplete)
    {
        super(aType, aFeature, aStudy, aCasGroupIds, aExcludeIncomplete);

        allSets = aTaggedConfigurations;
        diff = aDiff;
    }

    public boolean isScoringSingleValueItems()
    {
        return scoringSingleValueItems;
    }

    public void setScoringSingleValueItems(boolean aScoringSingleValueItems)
    {
        scoringSingleValueItems = aScoringSingleValueItems;
    }

    public boolean noPositions()
    {
        return study.getItemCount() == 0;
    }

    /**
     * @return Positions that were not seen in all CAS groups.
     */
    public List<ConfigurationSet> getIncompleteSetsByPosition()
    {
        return allSets.stream() //
                .filter(s -> s.hasTag(Tag.INCOMPLETE_POSITION)) //
                .toList();
    }

    /**
     * @return Positions that were seen in all CAS groups, but labels are unset (null).
     */
    public List<ConfigurationSet> getIncompleteSetsByLabel()
    {
        return allSets.stream() //
                .filter(s -> s.hasTag(Tag.INCOMPLETE_LABEL)) //
                .toList();
    }

    public List<ConfigurationSet> getPluralitySets()
    {
        return allSets.stream() //
                .filter(s -> s.hasTag(Tag.STACKED)) //
                .toList();
    }

    /**
     * @return sets differing with respect to the type and feature used to calculate agreement.
     */
    public List<ConfigurationSet> getSetsWithDifferences()
    {
        return allSets.stream() //
                .filter(s -> s.hasTag(Tag.DIFFERENCE)) //
                .toList();
    }

    public List<ConfigurationSet> getCompleteSets()
    {
        return allSets.stream() //
                .filter(s -> s.hasTag(Tag.COMPLETE)) //
                .toList();
    }

    public List<ConfigurationSet> getIrrelevantSets()
    {
        return allSets.stream() //
                .filter(s -> s.hasTag(Tag.IRRELEVANT)) //
                .toList();
    }

    public List<ConfigurationSet> getRelevantSets()
    {
        return allSets.stream() //
                .filter(s -> !s.hasTag(Tag.IRRELEVANT)) //
                .toList();

    }

    public List<ConfigurationSet> getUsedSets()
    {
        return allSets.stream() //
                .filter(s -> !s.hasTag(Tag.USED)) //
                .toList();
    }

    public List<ConfigurationSet> getAllSets()
    {
        return allSets;
    }

    public DiffResult getDiff()
    {
        return diff;
    }

    public Set<Object> getObservedCategories()
    {
        var observedCategories = new HashSet<Object>();
        for (var item : study.getItems()) {
            for (var unit : item.getUnits()) {
                var category = unit.getCategory();
                if (category != null) {
                    observedCategories.add(category);
                }
            }
        }
        return observedCategories;
    }

    @Override
    public boolean isAllNull(String aCasGroupId)
    {
        for (var item : study.getItems()) {
            if (item.getUnit(getCasGroupIds().indexOf(aCasGroupId)).getCategory() != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long getNonNullCount(String aCasGroupId)
    {
        var i = 0;
        for (var item : study.getItems()) {
            if (item.getUnit(getCasGroupIds().indexOf(aCasGroupId)).getCategory() != null) {
                i++;
            }
        }
        return i;
    }

    @Override
    public boolean isEmpty()
    {
        return study.getItemCount() == 0;
    }

    @Override
    public long getItemCount(String aRater)
    {
        return study.getItemCount();
    }

    @Override
    public String toString()
    {
        return "CodingAgreementResult [type=" + type + ", feature=" + feature + ", agreement="
                + agreement + "]";
    }
}
