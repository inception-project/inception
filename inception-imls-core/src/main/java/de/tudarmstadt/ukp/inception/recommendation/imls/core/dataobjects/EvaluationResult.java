/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation.RetrievableResults;

public class EvaluationResult implements RetrievableResults, Serializable
{
    private static final long serialVersionUID = 1180936232526047313L;
    String id;
    private List<ExtendedResult> knownDataResult = new LinkedList<>();
    private List<ExtendedResult> unknownDataResult = new LinkedList<>();
    
    public EvaluationResult(String id)
    {
        super();
        this.id = id;
    }

    public EvaluationResult(String id, List<ExtendedResult> knownDataResult,
            List<ExtendedResult> unknownDataResult)
    {
        super();
        this.id = id;
        this.knownDataResult = knownDataResult;
        this.unknownDataResult = unknownDataResult;
    }



    public void setKnownDataResult(List<ExtendedResult> knownDataResult)
    {
        this.knownDataResult = knownDataResult;
    }

    public void setUnknownDataResult(List<ExtendedResult> unknownDataResult)
    {
        this.unknownDataResult = unknownDataResult;
    }
    
    public String getId() {
        return id;
    }

    @Override
    public List<ExtendedResult> getKnownDataResults()
    {
        return knownDataResult;
    }

    @Override
    public List<ExtendedResult> getUnknownDataResults()
    {
        return unknownDataResult;
    }

}
