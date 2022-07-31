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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ExternalResultDataProvider
    extends SortableDataProvider<ExternalSearchResult, String>
{
    private static final long serialVersionUID = 5594618512472876346L;

    private List<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

    private ExternalSearchService externalSearchService;

    private User user;

    public ExternalResultDataProvider(ExternalSearchService aExternalSearchService, User aUser)
    {
        // // Set default sort
        // setSort("documentTitle", SortOrder.ASCENDING);

        // Set external search service
        externalSearchService = aExternalSearchService;

        // Set user
        user = aUser;
    }

    @Override
    public Iterator<ExternalSearchResult> iterator(long first, long count)
    {
        return results.subList((int) first, (int) (first + count)).iterator();
    }

    /**
     * @see org.apache.wicket.markup.repeater.data.IDataProvider#size()
     */
    @Override
    public long size()
    {
        return results.size();
    }

    /**
     * @see org.apache.wicket.markup.repeater.data.IDataProvider#model(java.lang.Object)
     */
    @Override
    public IModel<ExternalSearchResult> model(ExternalSearchResult object)
    {
        return new LoadableDetachableModel<ExternalSearchResult>(object)
        {

            private static final long serialVersionUID = -8141568381625089300L;

            @Override
            protected ExternalSearchResult load()
            {
                return object;
            }
        };
    }

    public void searchDocuments(DocumentRepository aRepository, String aQuery) throws IOException
    {
        results.clear();

        // No query, no results
        if (StringUtils.isBlank(aQuery)) {
            return;
        }

        results.addAll(externalSearchService.query(user, aRepository, aQuery));
    }
}
