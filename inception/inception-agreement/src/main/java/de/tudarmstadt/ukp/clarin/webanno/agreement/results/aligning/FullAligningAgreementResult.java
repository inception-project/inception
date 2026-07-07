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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.aligning;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import org.dkpro.statistics.agreement.aligning.AlignableAnnotationUnit;
import org.dkpro.statistics.agreement.aligning.AligningAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.FullAgreementResult_ImplBase;

public class FullAligningAgreementResult
    extends FullAgreementResult_ImplBase<AligningAnnotationStudy>
{
    private static final long serialVersionUID = -3672534728485229289L;

    public FullAligningAgreementResult(String aType, String aFeature,
            AligningAnnotationStudy aStudy, List<String> aCasGroupIds, boolean aExcludeIncomplete)
    {
        super(aType, aFeature, aStudy, aCasGroupIds, aExcludeIncomplete);
    }

    @Override
    public boolean isAllNull(String aRater)
    {
        return study.getUnits().stream() //
                .filter(u -> aRater.equals(u.getRater().getName())) //
                .noneMatch(FullAligningAgreementResult::hasLabel);
    }

    @Override
    public long getNonNullCount(String aRater)
    {
        return study.getUnits().stream() //
                .filter(u -> aRater.equals(u.getRater().getName())) //
                .filter(FullAligningAgreementResult::hasLabel) //
                .count();
    }

    @Override
    public long getItemCount(String aRater)
    {
        return study.getUnits().stream() //
                .filter(u -> aRater.equals(u.getRater().getName())) //
                .count();
    }

    @Override
    public boolean isEmpty()
    {
        return study.getUnitCount() == 0;
    }

    private static boolean hasLabel(AlignableAnnotationUnit aUnit)
    {
        return aUnit.getFeatureNames().stream() //
                .map(aUnit::getFeatureValue) //
                .anyMatch(v -> isNotEmpty(v));
    }
}
