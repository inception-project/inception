/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface ImportExportService
{
    String SERVICE_NAME = "importExportService";
    
    // --------------------------------------------------------------------------------------------
    // Methods related to import/export data formats
    // --------------------------------------------------------------------------------------------

    /**
     * Returns the labels on the UI for the format of the {@link SourceDocument} to be read from a
     * properties File
     *
     * @return labels of readable formats.
     */
    List<String> getReadableFormatLabels();

    /**
     * Returns the Id of the format for the {@link SourceDocument} to be read from a properties File
     *
     * @param label
     *            the label.
     *
     * @return the ID.
     */
    String getReadableFormatId(String label);

    /**
     * Returns formats of the {@link SourceDocument} to be read from a properties File
     *
     * @return the formats.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    Map<String, Class<CollectionReader>> getReadableFormats()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the labels on the UI for the format of {@link AnnotationDocument} while exporting
     *
     * @return the labels.
     */
    List<String> getWritableFormatLabels();

    /**
     * Returns the Id of the format for {@link AnnotationDocument} while exporting
     *
     * @param label
     *            the label.
     * @return the ID.
     */
    String getWritableFormatId(String label);

    /**
     * Returns formats of {@link AnnotationDocument} while exporting
     *
     * @return the formats.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    Map<String, Class<JCasAnnotator_ImplBase>> getWritableFormats()
        throws IOException, ClassNotFoundException;
    
    /**
     * Convert a file to a CAS.
     *
     * @param aFile
     *            the file.
     * @param aProject
     *            the project to which this file belongs (required to get the type system).
     * @param aFormat
     *            ID of a supported file format
     * @return the JCas.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    JCas importCasFromFile(File aFile, Project aProject, String aFormat)
        throws UIMAException, IOException, ClassNotFoundException;

    File exportCasToFile(CAS cas, SourceDocument aDocument, String aFileName,
            Class aWriter, boolean aStripExtension)
        throws IOException, UIMAException;
    
    /**
     * Exports an {@link AnnotationDocument } CAS Object as TCF/TXT/XMI... file formats.
     *
     * @param document
     *            The {@link SourceDocument} where we get the id which hosts both the source
     *            Document and the annotated document
     * @param user
     *            the {@link User} who annotates the document.
     * @param writer
     *            the DKPro Core writer.
     * @param fileName
     *            the file name.
     * @param mode
     *            the mode.
     * @return a temporary file.
     * @throws UIMAException
     *             if there was a conversion error.
     * @throws IOException
     *             if there was an I/O error.
     * @throws ClassNotFoundException
     *             if the DKPro Core writer could not be found.
     */
    @SuppressWarnings("rawtypes")
    File exportAnnotationDocument(SourceDocument document, String user, Class writer,
            String fileName, Mode mode)
        throws UIMAException, IOException, ClassNotFoundException;

    @SuppressWarnings("rawtypes")
    File exportAnnotationDocument(SourceDocument document, String user, Class writer,
            String fileName, Mode mode, boolean stripExtension)
        throws UIMAException, IOException, ClassNotFoundException;
}
