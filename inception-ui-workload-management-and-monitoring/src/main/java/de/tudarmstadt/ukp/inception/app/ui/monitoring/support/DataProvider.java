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

package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.*;

import java.io.Serializable;
import java.util.*;

public class DataProvider extends SortableDataProvider<List<String>, Object>
{
    private List<List<String>> data = new ArrayList<>();
    private List<String> headers;
    private int size;
    private SortableDataProviderComparator comparator = new SortableDataProviderComparator();
    private IModel<List<List<String>>> model;


    public DataProvider(List<String> aTableHeaders, List<List<String>> aContents)
    {
        this.headers = aTableHeaders;
        this.size = aContents.size();
        this.data = aContents;

        //Required
        model = new LoadableDetachableModel<List<List<String>>>()
        {
            @Override
            protected List<List<String>> load()
            {
                return data;
            }
        };
    }

    public Iterator<List<String>> iterator(long aFirst, long aCount)
    {

        List<List<String>> newList = new ArrayList<>(data);

        //TODO Sort data
        //Collections.sort(newList, comparator);

        //Return data
        return newList.subList((int)aFirst, (int)(aFirst + aCount)).iterator();
    }

    @Override
    public long size()
    {
        return size;
    }

    @Override
    public IModel<List<String>> model(List<String> aObject)
    {
        return Model.ofList(aObject);
    }

    @Override
    public void detach()
    {
        model.detach();
        super.detach();

    }

    public List<String> getTableHeaders()
    {
        return headers;
    }

    private class SortableDataProviderComparator implements Comparator<SourceDocument>, Serializable
    {
        //TODO sorting
        public int compare(final SourceDocument aDoc1, final SourceDocument aDoc2)
        {
            PropertyModel<Comparable> model1 = new PropertyModel<Comparable>(aDoc1, getSort().getProperty().toString());
            PropertyModel<Comparable> model2 = new PropertyModel<Comparable>(aDoc2, getSort().getProperty().toString());

            int result = model1.getObject().compareTo(model2.getObject());

            if (!getSort().isAscending())
            {
                result = -result;
            }
            return result;
        }

        @Override
        public Comparator<SourceDocument> reversed()
        {
            return null;
        }
    }
}
