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
package de.tudarmstadt.ukp.inception.io.tei;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.request.resource.ResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils;

public class TeiXmlDocumentFormatSupport
    implements FormatSupport
{
    public static final String ID = "tei-xml-document";
    public static final String NAME = "TEI P5 XML (experimental)";

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
    public List<ResourceReference> getCssStylesheets()
    {
        return asList(TeiXmlDocumentCssReference.get());
    }

    @Override
    public List<String> getSectionElements()
    {
        return asList("p", "lg", "biblStruct");
    }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(TeiXmlDocumentReader.class, aTSD);
    }

    @Override
    public Optional<PolicyCollection> getPolicy() throws IOException
    {
        return new WatchedResourceFile<PolicyCollection>(
                getClass().getResource("TeiXmlDocumentPolicy.yaml"),
                PolicyCollectionIOUtils::loadPolicies).get();
    }

    @Override
    public void prepareAnnotationCas(CAS aInitialCas, SourceDocument aDocument)
    {
        XmlNodeUtils.removeXmlDocumentStructure(aInitialCas);
    }
}
