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

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.eclipse.rdf4j.sail.lucene.AbstractReaderMonitor;

/**
 * ReaderMonitor holds IndexReader and IndexSearcher. When ReaderMonitor is closed it do not close
 * IndexReader and IndexSearcher as long as someone reads from them. Variable readingCount remember
 * how many times it was read.
 *
 * @author Tomasz Trela, DFKI Gmbh
 */
public class ReaderMonitor
    extends AbstractReaderMonitor
{

    /**
     * The IndexSearcher that can be used to query the current index' contents.
     */
    private IndexSearcher indexSearcher;

    private IOException indexSearcherCreateException;

    /**
     * If exception occur when create indexReader it will be thrown on getIndexReader or get
     * IndexSearcher
     *
     * @param index
     * @param directory
     *            Initializes IndexReader
     */
    public ReaderMonitor(final LuceneIndex index, Directory directory)
    {
        super(index);
        try {
            IndexReader indexReader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(indexReader);
        }
        catch (IOException e) {
            indexSearcherCreateException = e;
        }
    }

    /**
     * @throws IOException
     */
    @Override
    protected void handleClose() throws IOException
    {
        try {
            if (indexSearcher != null) {
                indexSearcher.getIndexReader().close();
            }
        }
        finally {
            indexSearcher = null;
        }
    }

    // //////////////////////////////Methods for controlled index access

    protected IndexSearcher getIndexSearcher() throws IOException
    {
        if (indexSearcherCreateException != null) {
            throw indexSearcherCreateException;
        }
        return indexSearcher;
    }

}
