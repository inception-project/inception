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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.ArrayList;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.api.pdf.type.PdfChunk;
import org.dkpro.core.api.pdf.type.PdfPage;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.pdfeditor2.config.PdfAnnotationEditor2SupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.pdfeditor2.config.PdfFormatProperties;

/**
 * Support for PDF file format.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PdfAnnotationEditor2SupportAutoConfiguration#pdfFormat2Support}.
 * </p>
 */
public class PdfFormatSupport
    implements FormatSupport
{
    public static final String ID = "pdf2";
    public static final String NAME = "PDF";

    private final PdfFormatProperties properties;

    public PdfFormatSupport(PdfFormatProperties aProperties)
    {
        properties = aProperties;
    }

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
    public boolean isReadable()
    {
        return true;
    }

    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(VisualPdfReader.class, aTSD, //
                VisualPdfReader.PARAM_ADD_MORE_FORMATTING, properties.isAddMoreFormatting(), //
                VisualPdfReader.PARAM_AVERAGE_CHAR_TOLERANCE, properties.getAverageCharTolerance(), //
                VisualPdfReader.PARAM_DROP_THRESHOLD, properties.getDropThreshold(), //
                VisualPdfReader.PARAM_GENERATE_HTML_STRUCTURE, properties.isGenerateHtmlStructure(), //
                VisualPdfReader.PARAM_INDENT_THRESHOLD, properties.getIndentThreshold(), //
                VisualPdfReader.PARAM_SHOULD_SEPARATE_BY_BEADS,
                properties.isShouldSeparateByBeads(), //
                VisualPdfReader.PARAM_SORT_BY_POSITION, properties.isSortByPosition(), //
                VisualPdfReader.PARAM_SPACING_TOLERANCE, properties.getSpacingTolerance(), //
                VisualPdfReader.PARAM_SUPPRESS_DUPLICATE_OVERLAPPING_TEXT,
                properties.isSuppressDuplicateOverlappingText());
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        return createEngineDescription( //
                VisualPdfWriter.class, aTSD, //
                VisualPdfWriter.PARAM_PROJECT_ID, aProject.getId());
    }

    @Override
    public void prepareAnnotationCas(CAS aCas, SourceDocument aDocument)
    {
        removePdfLayout(aCas);
    }

    public static int removePdfLayout(CAS aCas)
    {
        var toDelete = new ArrayList<FeatureStructure>();
        aCas.select(PdfChunk.class).forEach(toDelete::add);
        aCas.select(PdfPage.class).forEach(toDelete::add);
        toDelete.forEach(aCas::removeFsFromIndexes);
        return toDelete.size();
    }
}
