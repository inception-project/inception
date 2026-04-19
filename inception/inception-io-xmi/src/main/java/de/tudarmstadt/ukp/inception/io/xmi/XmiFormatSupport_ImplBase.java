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
package de.tudarmstadt.ukp.inception.io.xmi;

import static de.tudarmstadt.ukp.inception.support.io.ZipUtils.zipFolder;
import static java.io.File.createTempFile;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.addConfigurationParameters;
import static org.apache.uima.fit.util.LifeCycleUtil.collectionProcessComplete;
import static org.apache.uima.fit.util.LifeCycleUtil.destroy;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;

import de.tudarmstadt.ukp.clarin.webanno.api.format.UimaReaderWriterFormatSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class XmiFormatSupport_ImplBase
    extends UimaReaderWriterFormatSupport_ImplBase
{
    @Override
    public final boolean isProneToInconsistencies()
    {
        return true;
    }

    @Override
    public File write(SourceDocument aDocument, CAS aCas, File aTargetFolder,
            boolean aStripExtension)
        throws ResourceInitializationException, AnalysisEngineProcessException, IOException
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

        // The DKPro XmiWriter produces not only the XMI file but also a typesystem.xml file.
        // So we ZIP up the target folder and return that ZIP file as the export result.
        var exportFile = createTempFile("inception-document", ".zip");
        zipFolder(aTargetFolder, exportFile);
        return exportFile;
    }

}
