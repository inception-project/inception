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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static java.io.File.createTempFile;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.wicket.request.resource.ResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;
import de.tudarmstadt.ukp.inception.support.uima.CasObfuscationUtils;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

public interface FormatSupport
{
    Set<String> DEFAULT_PERMITTED_RESOURCE_EXTENSIONS = unmodifiableSet(
            Set.of("apng", "avif", "gif", "jpg", "jpeg", "jfif", "pjpeg", "pjp", "png", "svg",
                    "webp", "bmp", "tif", "tiff", "mp3", "mp4"));

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
     * @return whether the format can be read (i.e. {@link #read} is implemented).
     */
    default boolean isReadable()
    {
        return false;
    }

    /**
     * @return whether the format can be written (i.e. {@link #write} is implemented).
     */
    default boolean isWritable()
    {
        return false;
    }

    /**
     * @return whether the format is prone to inconsistencies. Formats that offer a much flexibility
     *         to the user (e.g. CAS XMI) are also prone to letting the user import inconsistent
     *         data. When importing documents from such formats, the CAS Doctor should be applied to
     *         check for inconsistencies.
     */
    default boolean isProneToInconsistencies()
    {
        return true;
    }

    default boolean hasResources()
    {
        return false;
    }

    default boolean isAccessibleResource(File aDocFile, String aResourcePath)
    {
        return DEFAULT_PERMITTED_RESOURCE_EXTENSIONS.contains(getExtension(aResourcePath));
    }

    default InputStream openResourceStream(File aDocFile, String aResourcePath) throws IOException
    {
        return ZipUtils.openResourceStream(aDocFile, aResourcePath);
    }

    /**
     * @return format-specific CSS style-sheets that styleable editors should load.
     */
    default List<ResourceReference> getCssStylesheets()
    {
        return emptyList();
    }

    /**
     * @return format-specific JavaScript resources that scriptable editors should load into the
     *         document context before the editor itself initializes. Used e.g. to expose a factory
     *         for a {@code DocumentStructureStrategy} that the editor will pull via
     *         {@link #getDocumentStructureFactory()}.
     */
    default List<ResourceReference> getJavaScripts()
    {
        return emptyList();
    }

    /**
     * @return a JavaScript expression that evaluates to a factory which produces a
     *         {@code DocumentStructureStrategy} for this format. The expression is evaluated in the
     *         editor's iframe context after the scripts returned by {@link #getJavaScripts()} have
     *         loaded. {@link Optional#empty()} (the default) means no format-specific strategy and
     *         the editor falls back to a no-op.
     */
    default Optional<String> getDocumentStructureFactory()
    {
        return empty();
    }

    /**
     * @return format-specific section elements
     */
    default Set<String> getSectionElements()
    {
        return Set.of("p");
    }

    /**
     * @return format-specific protected elements
     */
    default Set<String> getProtectedElements()
    {
        return emptySet();
    }

    default Optional<PolicyCollection> getPolicy() throws IOException
    {
        return empty();
    }

    default InputStream obfuscate(SourceDocument aDocument, InputStream aSource) throws IOException
    {
        if (!(isReadable() && isWritable())) {
            closeQuietly(aSource);
            throw new UnsupportedOperationException(
                    "The format [" + getName() + "] cannot be obfuscated");
        }

        if (aSource == null) {
            return null;
        }

        File srcFile = null;
        File targetFolder = null;

        try {
            srcFile = createTempFile("inception-format-src", ".tmp");
            copy(aSource, srcFile.toPath(), REPLACE_EXISTING);

            try (var session = CasStorageSession.openNested()) {
                var jcas = JCasFactory.createJCas();
                read(aDocument, jcas.getCas(), srcFile);

                // Some exporters need access to the original source document in order
                // to merge their data with the original data.
                addOrUpdateDocumentMetadata(jcas.getCas(), aDocument, INITIAL_SET.id());

                var obfCas = CasObfuscationUtils.createObfuscatedClone(jcas.getCas());

                targetFolder = createTempDirectory("inception-format-obf").toFile();
                var exportFile = write(aDocument, obfCas, targetFolder, false);

                // At this point, we don't need to delete the target folder anymore due to
                // an exception. So we transfer responsibility for cleanup to the caller via the
                // returned stream and set targetFolder to null to avoid double deletion in the
                // finally block.
                // Make sure we open the stream before setting targetFolder to null so we can still
                // clean up if there is an error when opening the stream.
                var cleanupFolder = targetFolder;
                var exportIs = newInputStream(exportFile.toPath());
                targetFolder = null;

                // Stream from the temp file and defer cleanup to stream close so we
                // don't need to buffer the entire obfuscated document in memory.
                return new FilterInputStream(exportIs)
                {
                    @Override
                    public void close() throws IOException
                    {
                        try {
                            super.close();
                        }
                        finally {
                            deleteQuietly(cleanupFolder);
                        }
                    }
                };
            }
            catch (UIMAException e) {
                throw new IOException(e);
            }
        }
        finally {
            deleteQuietly(targetFolder);
            deleteQuietly(srcFile);
            closeQuietly(aSource);
        }
    }

    void read(SourceDocument aDocument, CAS cas, File aFile) throws UIMAException, IOException;

    File write(SourceDocument aDocument, CAS aCas, File aTargetFolder, boolean aStripExtension)
        throws UIMAException, IOException;

    /**
     * Apply to an initial CAS created from a source document in order to prepare it for being an
     * annotation CAS for an annotator. It will modify the provided CAS in place, so make sure it is
     * already the copy that will be given to the annotator!
     * 
     * @param aCas
     *            the annotators CAS copied from the initial CAS.
     * @param aDocument
     *            the source document this CAS belongs to
     */
    default void prepareAnnotationCas(CAS aCas, SourceDocument aDocument)
    {
        // Do nothing by default
    }

    static void addOrUpdateDocumentMetadata(CAS aCas, SourceDocument aDocument, String aDataOwner)
        throws MalformedURLException, CASException
    {
        var slug = aDocument.getProject().getSlug();
        var documentMetadata = DocumentMetaData.get(aCas.getJCas());
        documentMetadata.setDocumentTitle(aDocument.getName());
        documentMetadata.setCollectionId(slug);
        documentMetadata.setDocumentId(aDataOwner);
        documentMetadata.setDocumentBaseUri(slug);
        documentMetadata.setDocumentUri(slug + "/" + aDocument.getName());
    }
}
