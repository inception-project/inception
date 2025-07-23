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
package de.tudarmstadt.ukp.inception.pdfeditor2.format;

import static de.tudarmstadt.ukp.inception.pdfeditor2.format.VisualPdfWriter.CAS_XMI_XML_1_0;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.pdfeditor2.config.PdfAnnotationEditor2SupportAutoConfiguration;

/**
 * Support for PDF file format.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PdfAnnotationEditor2SupportAutoConfiguration#pdfXmiCasFormatSupport}.
 * </p>
 */
public class PdfXmiCasFormatSupport
    implements FormatSupport
{
    public static final String ID = "pdf2XmiCas";
    public static final String NAME = "PDF (with embedded XMI CAS)";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        return createEngineDescription( //
                VisualPdfWriter.class, aTSD, //
                VisualPdfWriter.PARAM_EMBEDDED_CAS_FORMAT, CAS_XMI_XML_1_0, //
                VisualPdfWriter.PARAM_PROJECT_ID, aProject.getId());
    }
}
