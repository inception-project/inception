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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.request.resource.ResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

public interface DocumentImportExportService
{
    // --------------------------------------------------------------------------------------------
    // Methods related to import/export data formats
    // --------------------------------------------------------------------------------------------

    List<FormatSupport> getFormats();

    default List<FormatSupport> getReadableFormats()
    {
        return getFormats().stream().filter(FormatSupport::isReadable).toList();
    }

    default List<FormatSupport> getWritableFormats()
    {
        return getFormats().stream().filter(FormatSupport::isWritable).toList();
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
        return getFormats().stream() //
                .filter(f -> f.getId().equals(aFormatId)) //
                .findFirst();
    }

    default Optional<FormatSupport> getFormatByName(String aFormatName)
    {
        return getFormats().stream().filter(f -> f.getName().equals(aFormatName)).findFirst();
    }

    FormatSupport getFallbackFormat();

    default List<ResourceReference> getFormatCssStylesheets(SourceDocument aDoc)
    {
        Optional<FormatSupport> maybeFormatSupport = getFormatById(aDoc.getFormat());
        if (!maybeFormatSupport.isPresent()) {
            return Collections.emptyList();
        }

        FormatSupport formatSupport = maybeFormatSupport.get();

        return formatSupport.getCssStylesheets();
    }

    default List<String> getSectionElements(SourceDocument aDoc)
    {
        Optional<FormatSupport> maybeFormatSupport = getFormatById(aDoc.getFormat());
        if (!maybeFormatSupport.isPresent()) {
            return Collections.emptyList();
        }

        FormatSupport formatSupport = maybeFormatSupport.get();

        return formatSupport.getSectionElements();
    }

    default Optional<PolicyCollection> getFormatPolicy(SourceDocument aDoc) throws IOException
    {
        Optional<FormatSupport> maybeFormatSupport = getFormatById(aDoc.getFormat());
        if (!maybeFormatSupport.isPresent()) {
            return Optional.empty();
        }

        FormatSupport formatSupport = maybeFormatSupport.get();

        return formatSupport.getPolicy();
    }

    // --------------------------------------------------------------------------------------------
    // Methods related to importing/exporting
    // --------------------------------------------------------------------------------------------

    /**
     * Convert a file to a CAS. This method collects the project's type system as part of the call.
     * It is not well-suited for bulk-imports. For these, use
     * {@link #importCasFromFile(File, SourceDocument, TypeSystemDescription)} instead.
     *
     * @param aFile
     *            the file.
     * @param aDocument
     *            the source document to which the file belongs ID of a supported file format
     * @return the CAS.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    CAS importCasFromFile(File aFile, SourceDocument aDocument) throws UIMAException, IOException;

    /**
     * Convert a file to a CAS. This method is good for bulk-importing because it accepts the
     * project type system as a parameter instead of collecting it on every call.
     *
     * @param aFile
     *            the file.
     * @param aDocument
     *            the source document to which the file belongs
     * @param aFullProjectTypeSystem
     *            the project type system. If this parameter is {@code null}, then the method will
     *            try to resolve the type system itself.
     * @return the CAS.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    CAS importCasFromFile(File aFile, SourceDocument aDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException;

    CAS importCasFromFile(File aFile, SourceDocument aDocument, String aFormat,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException;

    CAS importCasFromFileNoChecks(File aFile, SourceDocument aDocument)
        throws UIMAException, IOException;

    CAS importCasFromFileNoChecks(File aFile, SourceDocument aDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException;

    /**
     * Exports the given CAS to a file on disk.
     * 
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     * 
     * @param cas
     *            the CAS to export
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aDataOwner
     *            the user owning the CAS file
     * @param aFormat
     *            the format in which to export
     * @return the exported file
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws UIMAException
     *             if there was an UIMA-level problem
     */
    File exportCasToFile(CAS cas, SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat)
        throws IOException, UIMAException;

    /**
     * Exports the given CAS to a file on disk.
     * 
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     * 
     * @param cas
     *            the CAS to export
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aDataOwner
     *            the user owning the CAS file
     * @param aFormat
     *            the format in which to export
     * @param aStripExtension
     *            whether to strip the original file extension from the file name
     * @param aBulkOperationContext
     *            a bulk export context (used to speed up bulk exports)
     * @return the exported file
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws UIMAException
     *             if there was an UIMA-level problem
     */
    File exportCasToFile(CAS cas, SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, boolean aStripExtension,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws IOException, UIMAException;

    /**
     * Exports an {@link AnnotationDocument } CAS Object as TCF/TXT/XMI... file formats.
     *
     * @param document
     *            The {@link SourceDocument} where we get the id which hosts both the source
     *            Document and the annotated document
     * @param aDataOwner
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
    File exportAnnotationDocument(SourceDocument document, String aDataOwner, FormatSupport aFormat,
            String fileName, Mode mode)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * Exports an {@link AnnotationDocument } CAS Object as TCF/TXT/XMI... file formats.
     *
     * @param document
     *            The {@link SourceDocument} where we get the id which hosts both the source
     *            Document and the annotated document
     * @param aDataOwner
     *            the {@link User} who annotates the document.
     * @param aFormat
     *            the format.
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
    File exportAnnotationDocument(SourceDocument document, String aDataOwner, FormatSupport aFormat,
            Mode mode)
        throws UIMAException, IOException, ClassNotFoundException;

    File exportAnnotationDocument(SourceDocument document, String aDataOwner, FormatSupport aFormat,
            String fileName, Mode mode, boolean stripExtension,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws UIMAException, IOException;

    File exportAnnotationDocument(SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, Mode aMode, boolean aStripExtension,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws UIMAException, IOException;

    /**
     * @return a type system with all the types that should be present in an exported CAS. This
     *         means in particular that type internal to the application should <b>not</b> be
     *         included.
     * @param aProject
     *            the project
     * @throws ResourceInitializationException
     *             if there was an UIMA-level problem
     */
    TypeSystemDescription getTypeSystemForExport(Project aProject)
        throws ResourceInitializationException;

    TypeSystemDescription getExportSpecificTypes();
}
