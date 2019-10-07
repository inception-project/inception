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

import static java.util.Collections.unmodifiableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.dkpro.statistics.agreement.IAnnotationStudy;

public abstract class AgreementResult<T extends IAnnotationStudy>
    implements Serializable
{
    protected final String type;
    protected final String feature;
    protected final T study;
    protected final boolean excludeIncomplete;
    protected final List<String> casGroupIds;

    protected double agreement;

    public AgreementResult(String aType, String aFeature, T aStudy, List<String> aCasGroupIds,
            boolean aExcludeIncomplete)
    {
        type = aType;
        feature = aFeature;
        study = aStudy;
        casGroupIds = unmodifiableList(new ArrayList<>(aCasGroupIds));
        excludeIncomplete = aExcludeIncomplete;
    }
    
    public List<String> getCasGroupIds()
    {
        return casGroupIds;
    }
    
    public void setAgreement(double aAgreement)
    {
        agreement = aAgreement;
    }
    
    public double getAgreement()
    {
        return agreement;
    }
    
    public T getStudy()
    {
        return study;
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
}
