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
package de.tudarmstadt.ukp.inception.diam.editor.lazydetails;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;

/**
 * Response for the {@code normData} command.
 * 
 * This is essentially present in brat, but there {@code results} would be a member of an array
 * called {@code value}. We simplified this a bit here and in {@code visualizer_ui.js}.
 * 
 * @deprecated Need to check if we want to keep this for DIAM
 */
@Deprecated
public class LazyDetailsResponse
    extends AjaxResponse
{
    private List<LazyDetailQuery> results = new ArrayList<>();

    public LazyDetailsResponse(String aAction)
    {
        super(aAction);
    }

    public List<LazyDetailQuery> getResults()
    {
        return results;
    }

    public void setResults(List<LazyDetailQuery> aResult)
    {
        results = aResult;
    }

    public void addResult(LazyDetailQuery aResult)
    {
        results.add(aResult);
    }
}
