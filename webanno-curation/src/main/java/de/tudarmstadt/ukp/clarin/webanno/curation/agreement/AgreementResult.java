/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationItem;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;

public class AgreementResult
{
    private final String type;
    private final String feature;
    private final DiffResult diff;
    final ICodingAnnotationStudy study;
    private final List<ConfigurationSet> setsWithDifferences;
    private final List<ConfigurationSet> completeSets;
    private final List<ConfigurationSet> irrelevantSets;
    private final List<ConfigurationSet> incompleteSetsByPosition;
    private final List<ConfigurationSet> incompleteSetsByLabel;
    private final List<ConfigurationSet> pluralitySets;
    private double agreement;
    private List<String> casGroupIds;
    private final boolean excludeIncomplete;

    public AgreementResult(String aType, String aFeature)
    {
        type = aType;
        feature = aFeature;
        diff = null;
        study = null;
        setsWithDifferences = null;
        completeSets = null;
        irrelevantSets = null;
        incompleteSetsByPosition = null;
        incompleteSetsByLabel = null;
        pluralitySets = null;
        excludeIncomplete = false;
    }

    public AgreementResult(String aType, String aFeature, DiffResult aDiff,
            ICodingAnnotationStudy aStudy, List<String> aCasGroupIds,
            List<ConfigurationSet> aComplete,
            List<ConfigurationSet> aIrrelevantSets,
            List<ConfigurationSet> aSetsWithDifferences,
            List<ConfigurationSet> aIncompleteByPosition,
            List<ConfigurationSet> aIncompleteByLabel,
            List<ConfigurationSet> aPluralitySets,
            boolean aExcludeIncomplete)
    {
        type = aType;
        feature = aFeature;
        diff = aDiff;
        study = aStudy;
        setsWithDifferences = aSetsWithDifferences;
        completeSets = Collections.unmodifiableList(new ArrayList<>(aComplete));
        irrelevantSets = aIrrelevantSets;
        incompleteSetsByPosition = Collections.unmodifiableList(new ArrayList<>(
                aIncompleteByPosition));
        incompleteSetsByLabel = Collections
                .unmodifiableList(new ArrayList<>(aIncompleteByLabel));
        pluralitySets = Collections
                .unmodifiableList(new ArrayList<>(aPluralitySets));
        casGroupIds = Collections.unmodifiableList(new ArrayList<>(aCasGroupIds));
        excludeIncomplete = aExcludeIncomplete;
    }
    
    public List<String> getCasGroupIds()
    {
        return casGroupIds;
    }
    
    public boolean isAllNull(String aCasGroupId)
    {
        for (ICodingAnnotationItem item : study.getItems()) {
            if (item.getUnit(casGroupIds.indexOf(aCasGroupId)).getCategory() != null) {
                return false;
            }
        }
        return true;
    }
    
    public int getNonNullCount(String aCasGroupId)
    {
        int i = 0;
        for (ICodingAnnotationItem item : study.getItems()) {
            if (item.getUnit(casGroupIds.indexOf(aCasGroupId)).getCategory() != null) {
                i++;
            }
        }
        return i;
    }

    public void setAgreement(double aAgreement)
    {
        agreement = aAgreement;
    }
    
    /**
     * Positions that were not seen in all CAS groups.
     */
    public List<ConfigurationSet> getIncompleteSetsByPosition()
    {
        return incompleteSetsByPosition;
    }

    /**
     * Positions that were seen in all CAS groups, but labels are unset (null).
     */
    public List<ConfigurationSet> getIncompleteSetsByLabel()
    {
        return incompleteSetsByLabel;
    }

    public List<ConfigurationSet> getPluralitySets()
    {
        return pluralitySets;
    }
    
    /**
     * @return sets differing with respect to the type and feature used to calculate agreement.
     */
    public List<ConfigurationSet> getSetsWithDifferences()
    {
        return setsWithDifferences;
    }
    
    public List<ConfigurationSet> getCompleteSets()
    {
        return completeSets;
    }
    
    public List<ConfigurationSet> getIrrelevantSets()
    {
        return irrelevantSets;
    }
    
    public int getDiffSetCount()
    {
        return setsWithDifferences.size();
    }
    
    public int getUnusableSetCount()
    {
        return incompleteSetsByPosition.size() + incompleteSetsByLabel.size()
                + pluralitySets.size();
    }
    
    public Object getCompleteSetCount()
    {
        return completeSets.size();
    }

    public int getTotalSetCount()
    {
        return diff.getPositions().size();
    }
    
    public int getRelevantSetCount()
    {
        return diff.getPositions().size() - irrelevantSets.size();
    }
    
    public double getAgreement()
    {
        return agreement;
    }
    
    public ICodingAnnotationStudy getStudy()
    {
        return study;
    }
    
    public DiffResult getDiff()
    {
        return diff;
    }
    
    public String getType()
    {
        return type;
    }
    
    public String getFeature()
    {
        return feature;
    }
    
    public boolean isExcludeIncomplete()
    {
        return excludeIncomplete;
    }

    @Override
    public String toString()
    {
        return "AgreementResult [type=" + type + ", feature=" + feature + ", diffs="
                + getDiffSetCount() + ", unusableSets=" + getUnusableSetCount()
                + ", agreement=" + agreement + "]";
    }
}
