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
package de.tudarmstadt.ukp.clarin.webanno.api.format;

import static de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils.zipFolder;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.addConfigurationParameters;
import static org.apache.uima.fit.util.LifeCycleUtil.collectionProcessComplete;
import static org.apache.uima.fit.util.LifeCycleUtil.destroy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.util.LifeCycleUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.request.resource.CssResourceReference;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.io.ResourceCollectionReaderBase;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface FormatSupport
{
    /**
     * Returns the format identifier which is stored in in {@link SourceDocument#setFormat(String)}.
     * 
     * @return the format identifier.
     */
    String getId();

    /**
     * @return a format name displayed in the UI.
     */
    String getName();

    /**
     * @return whether the format can be reader (i.e. {@link #getReaderDescription} is implemented).
     */
    default boolean isReadable()
    {
        return false;
    }

    /**
     * @return whether the format can be written (i.e. {@link #getWriterDescription} is
     *         implemented).
     */
    default boolean isWritable()
    {
        return false;
    }

    /**
     * @return format-specific CSS style-sheets that styleable editors should load.
     */
    default List<CssResourceReference> getCssStylesheets()
    {
        return Collections.emptyList();
    }

    /**
     * @param aTSD
     *            the project's type system
     * @return a UIMA reader description.
     * @throws ResourceInitializationException
     *             if the reader could not be initialized
     */
    default CollectionReaderDescription getReaderDescription(TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be read");
    }

    /**
     * @param aProject
     *            the project
     * @param aTSD
     *            the project's type system
     * @param aCAS
     *            the CAS to be exported
     * @return a UIMA reader description.
     * @throws ResourceInitializationException
     *             if the writer could not be initialized
     */
    default AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be written");
    }

    default void read(CAS cas, File aFile)
        throws ResourceInitializationException, IOException, CollectionException
    {
        CollectionReaderDescription readerDescription = getReaderDescription(null);
        addConfigurationParameters(readerDescription,
                ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
                aFile.getParentFile().getAbsolutePath(),
                ResourceCollectionReaderBase.PARAM_PATTERNS, "[+]" + aFile.getName());

        CollectionReader reader = null;
        try {
            reader = createReader(readerDescription);

            if (!reader.hasNext()) {
                throw new FileNotFoundException("Source file [" + aFile.getName()
                        + "] not found in [" + aFile.getPath() + "]");
            }
            reader.getNext(cas);
        }
        finally {
            LifeCycleUtil.close(reader);
            LifeCycleUtil.destroy(reader);
        }
    }

    default File write(SourceDocument aDocument, CAS aCas, File aTargetFolder,
            boolean aStripExtension)
        throws ResourceInitializationException, AnalysisEngineProcessException, IOException
    {
        AnalysisEngineDescription writer = getWriterDescription(aDocument.getProject(), null, aCas);
        addConfigurationParameters(writer, //
                JCasFileWriter_ImplBase.PARAM_USE_DOCUMENT_ID, true,
                JCasFileWriter_ImplBase.PARAM_ESCAPE_FILENAME, false,
                JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, aTargetFolder,
                JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, aStripExtension);

        // Not using SimplePipeline.runPipeline here now because it internally works
        // with an aggregate engine which is slow due to
        // https://issues.apache.org/jira/browse/UIMA-6200
        AnalysisEngine engine = null;
        try {
            engine = createEngine(writer);
            engine.process(aCas);
            collectionProcessComplete(engine);
        }
        finally {
            destroy(engine);
        }

        // If the writer produced more than one file, we package it up as a ZIP file
        File exportFile;
        if (aTargetFolder.listFiles().length > 1) {
            exportFile = new File(aTargetFolder.getAbsolutePath() + ".zip");
            zipFolder(aTargetFolder, exportFile);
        }
        else {
            exportFile = new File(aTargetFolder.getParent(),
                    aTargetFolder.listFiles()[0].getName());
            copyFile(aTargetFolder.listFiles()[0], exportFile);
        }

        return exportFile;
    }
}
