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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.query.TupleQueryResult;

public class TupleQueryResultDataProvider
    extends SortableDataProvider<Map<String, String>, Void>
    implements Serializable
{
    private static final long serialVersionUID = -4546564722894830886L;

    private List<Map<String, String>> data;

    public TupleQueryResultDataProvider()
    {
        data = Collections.emptyList();
    }

    public TupleQueryResultDataProvider(TupleQueryResult aTupleResult)
    {
        data = new ArrayList<Map<String, String>>();

        for (var bindingSet : aTupleResult) {
            var map = new LinkedHashMap<String, String>();

            for (var binding : bindingSet) {
                map.put(binding.getName(), String.valueOf(binding.getValue()));
            }

            data.add(map);
        }
    }

    @Override
    public Iterator<? extends Map<String, String>> iterator(long aFirst, long aCount)
    {
        return data.subList((int) aFirst, (int) (aFirst + aCount)).iterator();
    }

    @Override
    public long size()
    {
        return data.size();
    }

    @Override
    public IModel<Map<String, String>> model(Map<String, String> aObject)
    {
        return Model.ofMap(aObject);
    }
}
