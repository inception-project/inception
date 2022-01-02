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
package de.tudarmstadt.ukp.inception.htmleditor.docview;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.uima.cas.CAS;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;

public class HtmlDocumentView
    extends Label
{
    private static final long serialVersionUID = 4436249885266856565L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean DocumentService documentService;

    private IModel<AnnotationDocument> document;

    public HtmlDocumentView(String aId, IModel<AnnotationDocument> aDoc)
    {
        super(aId);
        document = aDoc;
        setEscapeModelStrings(false);
        setDefaultModel(LoadableDetachableModel.of(this::renderDocument));
    }

    private String renderDocument()
    {
        CAS cas;
        try {
            cas = documentService.readAnnotationCas(document.getObject());
        }
        catch (IOException e) {
            LOG.error("Unable to load data", e);
            getSession().error("Unable to load data: " + getRootCauseMessage(e));
            return "";
        }

        try {
            if (cas.select(XmlDocument.class).isEmpty()) {
                return new LegacyHtmlDocumentRenderer().renderHtmlDocumentStructure(cas);
            }

            return new HtmlDocumentRenderer().render(cas);
        }
        catch (Exception e) {
            LOG.error("Unable to render data", e);
            getSession().error("Unable to render data: " + getRootCauseMessage(e));
            return "";
        }
    }
}
