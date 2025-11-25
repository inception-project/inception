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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.externaleditor.policy.DefaultHtmlDocumentPolicy;
import de.tudarmstadt.ukp.inception.io.html.config.HtmlSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

/**
 * Support for HTML format.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link HtmlSupportAutoConfiguration#mhtmlFormatSupport}.
 * </p>
 */
public class MHtmlFormatSupport
    implements FormatSupport
{
    public static final String ID = "mhtml";
    public static final String NAME = "MHTML (Web archive)";

    private final DefaultHtmlDocumentPolicy defaultPolicy;

    public MHtmlFormatSupport(DefaultHtmlDocumentPolicy aDefaultPolicy)
    {
        defaultPolicy = aDefaultPolicy;
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
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(MHtmlDocumentReader.class, aTSD);
    }

    @Override
    public void prepareAnnotationCas(CAS aInitialCas, SourceDocument aDocument)
    {
        XmlNodeUtils.removeXmlDocumentStructure(aInitialCas);
    }

    @Override
    public List<String> getSectionElements()
    {
        return asList("p");
    }

    @Override
    public Optional<PolicyCollection> getPolicy() throws IOException
    {
        return Optional.of(defaultPolicy.getPolicy());
    }

    @Override
    public boolean hasResources()
    {
        return true;
    }

    @Override
    public InputStream openResourceStream(File aDocFile, String aResourcePath) throws IOException
    {
        try (var is = new FileInputStream(aDocFile)) {
            var builder = new DefaultMessageBuilder();
            var message = builder.parseMessage(is);
            var resourceBody = getResourcePartBody(message, aResourcePath);
            try (var baos = new ByteArrayOutputStream()) {
                resourceBody.getInputStream().transferTo(baos);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }
    }

    private static SingleBody getResourcePartBody(Message message, String aResourcePath)
        throws IOException
    {
        if (message.getBody() instanceof Multipart body) {
            var documentPart = body.getBodyParts().stream() //
                    .filter(e -> {
                        var field = e.getHeader().getField("Content-Location");
                        return field != null && aResourcePath.equals(field.getBody());
                    }) //
                    .findFirst();

            if (documentPart.isPresent()) {
                if (documentPart.get().getBody() instanceof SingleBody resourceBody) {
                    return resourceBody;
                }
            }
        }

        throw new FileNotFoundException("Resource not found [" + aResourcePath + "]");
    }
}
