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

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.addConfigurationParameters;
import static org.apache.uima.fit.util.LifeCycleUtil.collectionProcessComplete;
import static org.apache.uima.fit.util.LifeCycleUtil.destroy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.util.LifeCycleUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.io.ResourceCollectionReaderBase;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class UimaReaderWriterFormatSupport_ImplBase
    implements FormatSupport
{
    /**
     * @param aProject
     *            the project into which to import the document(s).
     * @param aTSD
     *            the project's type system
     * @return a UIMA reader description.
     * @throws ResourceInitializationException
     *             if the reader could not be initialized
     */
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
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
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        throw new UnsupportedOperationException("The format [" + getName() + "] cannot be written");
    }

    @Override
    public void read(SourceDocument aDocument, CAS cas, File aFile)
        throws ResourceInitializationException, IOException, CollectionException
    {
        var readerDescription = getReaderDescription(aDocument.getProject(), null);
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

    @Override
    public File write(SourceDocument aDocument, CAS aCas, File aTargetFolder,
            boolean aStripExtension)
        throws UIMAException, IOException
    {
        var writer = getWriterDescription(aDocument.getProject(), null, aCas);
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

        if (aTargetFolder.listFiles().length > 1) {
            throw new IOException("Writer are only allowed to produce exactly one output file. "
                    + "Writers producing mutiple files need to override write() with a suitable implementation.");
        }

        // If the writer produced only a single file, then that is the result
        var exportedFile = aTargetFolder.listFiles()[0];
        // temp-file prefix must be at least 3 chars
        var baseName = rightPad(getBaseName(exportedFile.getName()), 3, "_");
        var extension = getExtension(exportedFile.getName());
        var exportFile = createTempFile(baseName, "." + extension);
        copyFile(exportedFile, exportFile);
        return exportFile;
    }
}
