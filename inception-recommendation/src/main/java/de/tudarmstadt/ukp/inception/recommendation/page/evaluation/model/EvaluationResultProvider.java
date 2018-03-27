/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.page.evaluation.model;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;

public class EvaluationResultProvider
    implements IDataProvider<ExtendedResult>
{
    private static final long serialVersionUID = 8761568020144723725L;
    
    private List<ExtendedResult> results;
    
    public EvaluationResultProvider(List<ExtendedResult> results)
    {
        this.results = results;
    }
    
    @Override
    public void detach()
    {        
    }

    @Override
    public Iterator<? extends ExtendedResult> iterator(long first, long count)
    {
        return results.iterator();
    }

    @Override
    public long size()
    {
        return results.size();
    }

    @Override
    public IModel<ExtendedResult> model(ExtendedResult object)
    {
        return Model.of(object);
    }

}
