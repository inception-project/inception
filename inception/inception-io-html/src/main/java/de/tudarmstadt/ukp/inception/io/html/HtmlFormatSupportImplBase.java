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

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.externaleditor.policy.DefaultHtmlDocumentPolicy;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

public abstract class HtmlFormatSupportImplBase
    implements FormatSupport
{
    private static final Set<String> HTML_SECTION_ELEMENTS = Set.of("p");
    private static final Set<String> HTML_PROTECTED_ELEMENTS = Set.of( //
            "{http://www.w3.org/1998/Math/MathML}math", //
            "{http://www.w3.org/2000/svg}svg");

    private final DefaultHtmlDocumentPolicy defaultPolicy;

    public HtmlFormatSupportImplBase(DefaultHtmlDocumentPolicy aDefaultPolicy)
    {
        defaultPolicy = aDefaultPolicy;
    }

    @Override
    public Set<String> getProtectedElements()
    {
        return HTML_PROTECTED_ELEMENTS;
    }

    @Override
    public Set<String> getSectionElements()
    {
        return HTML_SECTION_ELEMENTS;
    }

    @Override
    public void prepareAnnotationCas(CAS aInitialCas, SourceDocument aDocument)
    {
        XmlNodeUtils.removeXmlDocumentStructure(aInitialCas);
    }

    @Override
    public Optional<PolicyCollection> getPolicy() throws IOException
    {
        return Optional.of(defaultPolicy.getPolicy());
    }
}
