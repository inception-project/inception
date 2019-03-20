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
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
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
    
    List<FormatSupport> getFormats();
    
    default List<FormatSupport> getReadableFormats()
    {
        return getFormats().stream().filter(FormatSupport::isReadable).collect(Collectors.toList());
    }

    default List<FormatSupport> getWritableFormats()
    {
        return getFormats().stream().filter(FormatSupport::isWritable).collect(Collectors.toList());
    }

    default Optional<FormatSupport> getReadableFormatById(String aFormatId)
    {
        return getFormats().stream().filter(f -> f.getId().equals(aFormatId) && f.isReadable())
                .findFirst();
    }

    default Optional<FormatSupport> getWritableFormatById(String aFormatId)
    {
        return getFormats().stream().filter(f -> f.getId().equals(aFormatId) && f.isWritable())
                .findFirst();
    }

    default Optional<FormatSupport> getWritableFormatByName(String aFormatName)
    {
        return getFormats().stream().filter(f -> f.getName().equals(aFormatName) && f.isWritable())
                .findFirst();
    }

    default Optional<FormatSupport> getFormatById(String aFormatId)
    {
        return getFormats().stream().filter(f -> f.getId().equals(aFormatId)).findFirst();
    }

    default Optional<FormatSupport> getFormatByName(String aFormatName)
    {
        return getFormats().stream().filter(f -> f.getName().equals(aFormatName)).findFirst();
    }
    
    // --------------------------------------------------------------------------------------------
    // Methods related to importing/exporting
    // --------------------------------------------------------------------------------------------
    
    /**
     * Convert a file to a CAS.
     *
     * @param aFile
     *            the file.
     * @param aProject
     *            the project to which this file belongs (required to get the type system).
     * @param aFormatId
     *            ID of a supported file format
     * @return the JCas.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    JCas importCasFromFile(File aFile, Project aProject, String aFormatId)
        throws UIMAException, IOException;

    /**
     * Exports the given CAS to a file on disk. 
     * 
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     */
    File exportCasToFile(CAS cas, SourceDocument aDocument, String aFileName, FormatSupport aFormat,
            boolean aStripExtension)
        throws IOException, UIMAException;
    
    /**
     * Exports an {@link AnnotationDocument } CAS Object as TCF/TXT/XMI... file formats. 
     *
     * @param document
     *            The {@link SourceDocument} where we get the id which hosts both the source
     *            Document and the annotated document
     * @param user
     *            the {@link User} who annotates the document.
     * @param aFormat
     *            the format.
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
    File exportAnnotationDocument(SourceDocument document, String user, FormatSupport aFormat,
            String fileName, Mode mode)
        throws UIMAException, IOException, ClassNotFoundException;

    File exportAnnotationDocument(SourceDocument document, String user, FormatSupport aFormat,
            String fileName, Mode mode, boolean stripExtension)
        throws UIMAException, IOException, ClassNotFoundException;
}
