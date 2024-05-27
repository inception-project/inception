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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing;

import java.util.List;

import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.FullAgreementResult_ImplBase;

public class FullUnitizingAgreementResult
    extends FullAgreementResult_ImplBase<IUnitizingAnnotationStudy>
{
    private static final long serialVersionUID = 2092691057728349705L;

    public FullUnitizingAgreementResult(String aType, String aFeature,
            IUnitizingAnnotationStudy aStudy, List<String> aCasGroupIds, boolean aExcludeIncomplete)
    {
        super(aType, aFeature, aStudy, aCasGroupIds, aExcludeIncomplete);
    }

    @Override
    public boolean isAllNull(String aRater)
    {
        var raterIdx = getCasGroupIds().indexOf(aRater);

        return study.getUnits().stream()
                .noneMatch(u -> u.getRaterIdx() == raterIdx && u.getCategory() != null);
    }

    @Override
    public long getNonNullCount(String aRater)
    {
        var raterIdx = getCasGroupIds().indexOf(aRater);

        return study.getUnits().stream() //
                .filter(u -> u.getRaterIdx() == raterIdx && u.getCategory() != null) //
                .count();
    }

    @Override
    public long getItemCount(String aRater)
    {
        var raterIdx = getCasGroupIds().indexOf(aRater);
        return study.getUnitCount(raterIdx);
    }

    @Override
    public boolean isEmpty()
    {
        return study.getUnitCount() == 0;
    }
}
