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

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.eclipse.rdf4j.sail.lucene.DocumentResult;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;

public class LuceneDocumentResult
    implements DocumentResult
{

    protected final ScoreDoc scoreDoc;

    protected final LuceneIndex index;

    private final Set<String> fields;

    private LuceneDocument fullDoc;

    public LuceneDocumentResult(ScoreDoc doc, LuceneIndex index, Set<String> fields)
    {
        this.scoreDoc = doc;
        this.index = index;
        this.fields = fields;
    }

    @Override
    public SearchDocument getDocument()
    {
        if (fullDoc == null) {
            Document doc = index.getDocument(scoreDoc.doc, fields);
            fullDoc = new LuceneDocument(doc, index.getSpatialStrategyMapper());
        }
        return fullDoc;
    }
}
