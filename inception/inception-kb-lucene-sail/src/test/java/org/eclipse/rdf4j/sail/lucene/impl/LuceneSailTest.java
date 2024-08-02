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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;

public class LuceneSailTest
    extends AbstractGenericLuceneTest
{

    private LuceneIndex index;

    @Override
    protected void configure(LuceneSail sail) throws IOException
    {
        index = new LuceneIndex(new ByteBuffersDirectory(), new StandardAnalyzer());
        sail.setLuceneIndex(index);
    }
}
