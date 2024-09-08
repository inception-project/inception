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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Highlighter;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.SearchFields;

import com.google.common.collect.Iterables;

public class LuceneDocumentScore
    extends LuceneDocumentResult
    implements DocumentScore
{

    private final Highlighter highlighter;

    private static Set<String> requiredFields(boolean all)
    {
        return all ? null : Collections.singleton(SearchFields.URI_FIELD_NAME);
    }

    public LuceneDocumentScore(ScoreDoc doc, Highlighter highlighter, LuceneIndex index)
    {
        super(doc, index, requiredFields(highlighter != null));
        this.highlighter = highlighter;
    }

    @Override
    public float getScore()
    {
        return scoreDoc.score;
    }

    @Override
    public boolean isHighlighted()
    {
        return (highlighter != null);
    }

    @Override
    public Iterable<String> getSnippets(final String field)
    {
        List<String> values = getDocument().getProperty(field);
        if (values == null) {
            return null;
        }
        return Iterables.transform(values,
                (String text) -> index.getSnippet(field, text, highlighter));
    }
}
