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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.DoubleStream;

import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.FullUnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class AgreementSummary
    implements Serializable
{
    private static final long serialVersionUID = 5994827594084192896L;

    private final String type;
    private final String feature;
    private final Set<String> casGroupIds = new HashSet<>();

    private final List<Double> agreements = new ArrayList<>();
    private final Set<Object> categories = new LinkedHashSet<>();
    private final Map<String, Long> itemCounts = new HashMap<>();
    private final Map<String, Long> nonNullContentCounts = new HashMap<>();
    private final Map<String, Boolean> allNull = new HashMap<>();

    private boolean empty;

    private int incompleteSetsByPosition;
    private int incompleteSetsByLabel;
    private int pluralitySets;
    private int relevantSetCount;
    private int completeSetCount;
    private int usedSetCount;

    public void merge(AgreementSummary aResult)
    {
        if (!type.equals(aResult.type)) {
            throw new IllegalArgumentException("All merged results must have the same type [" + type
                    + "] but encountered [" + aResult.type + "]");
        }

        if (!Objects.equals(feature, aResult.feature)) {
            throw new IllegalArgumentException("All merged results must have the same feature ["
                    + feature + "] but encountered [" + aResult.feature + "]");
        }

        casGroupIds.addAll(aResult.casGroupIds);
        agreements.addAll(aResult.agreements);
        categories.addAll(aResult.categories);

        for (var e : aResult.itemCounts.entrySet()) {
            itemCounts.merge(e.getKey(), e.getValue(), Long::sum);
        }

        for (var e : aResult.nonNullContentCounts.entrySet()) {
            nonNullContentCounts.merge(e.getKey(), e.getValue(), Long::sum);
        }

        for (var e : aResult.allNull.entrySet()) {
            allNull.merge(e.getKey(), e.getValue(), Boolean::logicalAnd);
        }

        empty &= aResult.empty;

        if (incompleteSetsByPosition >= 0 && aResult.incompleteSetsByPosition >= 0) {
            incompleteSetsByPosition += aResult.incompleteSetsByPosition;
        }

        if (incompleteSetsByLabel >= 0 && aResult.incompleteSetsByLabel >= 0) {
            incompleteSetsByLabel += aResult.incompleteSetsByLabel;
        }

        if (pluralitySets >= 0 && aResult.pluralitySets >= 0) {
            pluralitySets += aResult.pluralitySets;
        }

        if (relevantSetCount >= 0 && aResult.relevantSetCount >= 0) {
            relevantSetCount += aResult.relevantSetCount;
        }

        if (completeSetCount >= 0 && aResult.completeSetCount >= 0) {
            completeSetCount += aResult.completeSetCount;
        }

        if (usedSetCount >= 0 && aResult.usedSetCount >= 0) {
            usedSetCount += aResult.usedSetCount;
        }
    }

    public static AgreementSummary of(Serializable aResult)
    {
        if (aResult instanceof FullCodingAgreementResult result) {
            return new AgreementSummary(result);
        }

        if (aResult instanceof FullUnitizingAgreementResult result) {
            return new AgreementSummary(result);
        }

        throw new IllegalArgumentException(
                "Unsupported result type: [" + aResult.getClass().getName() + "]");
    }

    public static AgreementSummary skipped(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        var featureName = aFeature != null ? aFeature.getName() : null;
        return new AgreementSummary(aLayer.getName(), featureName);
    }

    public AgreementSummary(String aType, String aFeature)
    {
        type = aType;
        feature = aFeature;
        agreements.add(Double.NaN);
        empty = true;
    }

    public AgreementSummary(FullUnitizingAgreementResult aResult)
    {
        this((FullAgreementResult_ImplBase<?>) aResult);

        incompleteSetsByLabel = -1;
        incompleteSetsByPosition = -1;
        relevantSetCount = -1;
        completeSetCount = -1;
        usedSetCount = -1;
        pluralitySets = -1;
    }

    public AgreementSummary(FullCodingAgreementResult aResult)
    {
        this((FullAgreementResult_ImplBase<?>) aResult);

        incompleteSetsByLabel = aResult.getIncompleteSetsByLabel().size();
        incompleteSetsByPosition = aResult.getIncompleteSetsByPosition().size();
        pluralitySets = aResult.getPluralitySets().size();
        relevantSetCount = aResult.getRelevantSets().size();
        completeSetCount = aResult.getCompleteSets().size();

        usedSetCount = completeSetCount;
        if (!aResult.isExcludeIncomplete()) {
            usedSetCount += incompleteSetsByLabel + incompleteSetsByPosition;
        }
    }

    private AgreementSummary(FullAgreementResult_ImplBase<?> aResult)
    {
        type = aResult.getType();
        feature = aResult.getFeature();
        casGroupIds.addAll(aResult.casGroupIds);
        agreements.add(aResult.agreement);
        aResult.getCategories().forEach(categories::add);
        empty = aResult.isEmpty();

        for (var casGroupId : casGroupIds) {
            itemCounts.put(casGroupId, aResult.getItemCount(casGroupId));
            nonNullContentCounts.put(casGroupId, aResult.getNonNullCount(casGroupId));
            allNull.put(casGroupId, aResult.isAllNull(casGroupId));
        }
    }

    public List<String> getCasGroupIds()
    {
        return casGroupIds.stream().sorted().toList();
    }

    private DoubleStream usableAgreements()
    {
        return agreements.stream() //
                .mapToDouble(a -> a) //
                .filter(v -> !Double.isNaN(v)); // skip documents for which we have no agreement
    }

    public double getAgreement()
    {
        return usableAgreements().average().orElse(Double.NaN);
    }

    public long getTotalAgreementsCount()
    {
        return agreements.size();
    }

    public long getUsableAgreementsCount()
    {
        return usableAgreements().count();
    }

    public String getType()
    {
        return type;
    }

    public String getFeature()
    {
        return feature;
    }

    public boolean isEmpty()
    {
        return empty;
    }

    public long getItemCount(String aRater)
    {
        return itemCounts.getOrDefault(aRater, 0l);
    }

    public int getCategoryCount()
    {
        return categories.size();
    }

    public Long getNonNullCount(String aRater)
    {
        return nonNullContentCounts.getOrDefault(aRater, 0l);
    }

    public boolean isAllNull(String aRater)
    {
        return allNull.getOrDefault(aRater, true);
    }

    public int getIncompleteSetsByPosition()
    {
        return incompleteSetsByPosition;
    }

    public int getIncompleteSetsByLabel()
    {
        return incompleteSetsByLabel;
    }

    public int getRelevantSetCount()
    {
        return relevantSetCount;
    }

    public int getCompleteSetCount()
    {
        return completeSetCount;
    }

    public int getPluralitySets()
    {
        return pluralitySets;
    }

    public int getUsedSetCount()
    {
        return usedSetCount;
    }
}
