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
package de.tudarmstadt.ukp.inception.io.html;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.io.html.HtmlReader;

import de.tudarmstadt.ukp.clarin.webanno.api.format.UimaReaderWriterFormatSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * Support for HTML format.
 * <p>
 * This class is exposed as a Spring Component via
 * {@code HtmlAnnotationEditorSupportAutoConfiguration#htmlFormatSupport()}.
 * </p>
 */
public class LegacyHtmlFormatSupport
    extends UimaReaderWriterFormatSupport_ImplBase
{
    public static final String ID = "html";
    public static final String NAME = "HTML (legacy)";

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
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(HtmlReader.class, aTSD);
    }

    @Override
    public InputStream obfuscate(SourceDocument aDocument, InputStream aSource) throws IOException
    {
        // The HTML importer lifts the document structure into the INITIAL_CAS, so on re-import
        // the original source file is not consulted. We replace it with a minimal valid stand-in
        // in the obfuscated export.
        try {
            var standin = getClass().getResourceAsStream(OBFUSCATION_STANDIN_RESOURCE);
            if (standin == null) {
                throw new IOException("Missing obfuscation stand-in resource ["
                        + OBFUSCATION_STANDIN_RESOURCE + "]");
            }
            return standin;
        }
        finally {
            closeQuietly(aSource);
        }
    }

    private static final String OBFUSCATION_STANDIN_RESOURCE = //
            "LegacyHtmlFormatSupport-obfuscation-standin.html";
}
