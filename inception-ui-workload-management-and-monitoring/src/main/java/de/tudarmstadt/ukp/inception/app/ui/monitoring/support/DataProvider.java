/*
 * Copyright 2020
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;


public class DataProvider extends SortableDataProvider
    <SourceDocument, String> implements Serializable
{

    private List<String> headers;
    private List<SourceDocument> data;
    private IModel<List<SourceDocument>> model;


    public DataProvider(List<SourceDocument> aContents, List<String> headers)
    {

        this.data = aContents;
        this.headers = headers;


        //Initial Sorting
        setSort(headers.get(0), SortOrder.ASCENDING);

        //Required
        model = new LoadableDetachableModel<List<SourceDocument>>() {
            @Override
            protected List<SourceDocument> load() {
                return aContents;
            }
        };


    }
    @Override
    public Iterator<SourceDocument> iterator(long first, long count)
    {
        List<SourceDocument> newList = data;

        //

        //Sorting, check for which column was clicked and return new sorting accordingly
        //TODO rework of comparator needed
        Collections.sort(newList, (o1, o2) ->
        {
            int dir = getSort().isAscending() ? 1 : -1;
            if (getSort().getProperty().equals(headers.get(0)))
            {
                return dir * (o1.getName().compareTo(o2.getName()));

            } else if (getSort().getProperty().equals(headers.get(1)))
            {
                return dir * (o1.getName().compareTo(o2.getName()));

            }  else if (getSort().getProperty().equals(headers.get(2)))
            {
                return dir * (o1.getName().compareTo(o2.getName()));

            } else return 0;
        });

        return newList.subList((int)first, Math.
            min((int)first + (int)count, newList.size())).iterator();
    }

    @Override
    public long size()
    {
        return data.size();
    }

    @Override
    public IModel<SourceDocument> model(SourceDocument sourceDocument)
    {
        return Model.of(sourceDocument);
    }

    @Override
    public void detach()
    {
        model.detach();
        super.detach();
    }

}

