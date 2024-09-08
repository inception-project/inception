/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.impl;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.SearchQuery;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * To be removed, no longer used.
 */
@Deprecated
public class LuceneQuery
    implements SearchQuery
{

    private final Query query;

    private final LuceneIndex index;

    private Highlighter highlighter;

    @Deprecated
    public LuceneQuery(Query q, LuceneIndex index)
    {
        this.query = q;
        this.index = index;
    }

    @Override
    @Deprecated
    public Iterable<? extends DocumentScore> query(Resource resource) throws IOException
    {
        TopDocs docs;
        if (resource != null) {
            docs = index.search(resource, query);
        }
        else {
            docs = index.search(query);
        }
        return Iterables.transform(Arrays.asList(docs.scoreDocs), new Function<>()
        {

            @Override
            public DocumentScore apply(ScoreDoc doc)
            {
                return new LuceneDocumentScore(doc, highlighter, index);
            }
        });
    }

    @Override
    @Deprecated
    public void highlight(IRI property)
    {
        Formatter formatter = new SimpleHTMLFormatter(SearchFields.HIGHLIGHTER_PRE_TAG,
                SearchFields.HIGHLIGHTER_POST_TAG);
        highlighter = new Highlighter(formatter, new QueryScorer(query));
    }
}
