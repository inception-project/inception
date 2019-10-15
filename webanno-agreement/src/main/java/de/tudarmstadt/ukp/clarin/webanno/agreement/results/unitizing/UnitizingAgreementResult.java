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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing;

import java.util.List;

import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult;

public class UnitizingAgreementResult
    extends AgreementResult<IUnitizingAnnotationStudy>
{
    private static final long serialVersionUID = 2092691057728349705L;

    public UnitizingAgreementResult(String aType, String aFeature, IUnitizingAnnotationStudy aStudy,
            List<String> aCasGroupIds, boolean aExcludeIncomplete)
    {
        super(aType, aFeature, aStudy, aCasGroupIds, aExcludeIncomplete);
    }
}
