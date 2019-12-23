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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.NormalizationQueryResult;

/**
 * Response for the {@code normData} command.
 * 
 * This is essentially present in brat, but there {@code results} would be a member of an array 
 * called {@code value}. We simplified this a bit here and in {@code visualizer_ui.js}.
 */
public class NormDataResponse
    extends AjaxResponse
{
    public static final String COMMAND = "normData";
    
    private List<NormalizationQueryResult> results = new ArrayList<>();

    public NormDataResponse()
    {
        super(COMMAND);
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
    
    public List<NormalizationQueryResult> getResults()
    {
        return results;
    }

    public void setResults(List<NormalizationQueryResult> aResult)
    {
        results = aResult;
    }

    public void addResult(NormalizationQueryResult aResult)
    {
        results.add(aResult);
    }
}
