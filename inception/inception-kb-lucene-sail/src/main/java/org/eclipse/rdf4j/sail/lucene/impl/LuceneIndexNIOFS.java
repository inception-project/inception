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
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;

/**
 * LuceneIndex which uses a NIOFSDirectory instead of MMapDirectory to avoid the JVM crash (see
 * <a href= "http://stackoverflow.com/questions/8224843/jvm-crashes-on-lucene-datainput-readvint"
 * >http:// stackoverflow.com/questions/8224843/jvm-crashes-on-lucene-datainput- readvint</a>).
 *
 * @author andriy.nikolov
 */
public class LuceneIndexNIOFS
    extends LuceneIndex
{

    @Override
    protected Directory createDirectory(Properties parameters) throws IOException
    {
        if (parameters.containsKey(LuceneSail.LUCENE_DIR_KEY)) {
            return new NIOFSDirectory(Paths.get(parameters.getProperty(LuceneSail.LUCENE_DIR_KEY)));
        }
        else {
            return super.createDirectory(parameters);
        }
    }
}
