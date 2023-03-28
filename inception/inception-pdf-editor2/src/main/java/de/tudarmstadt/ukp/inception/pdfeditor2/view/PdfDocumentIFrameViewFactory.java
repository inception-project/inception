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
package de.tudarmstadt.ukp.inception.pdfeditor2.view;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewFactory;
import de.tudarmstadt.ukp.inception.pdfeditor2.config.PdfAnnotationEditor2SupportAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PdfAnnotationEditor2SupportAutoConfiguration#pdfDocument2IFrameViewFactory}.
 * </p>
 */
public class PdfDocumentIFrameViewFactory
    implements DocumentViewFactory
{
    public static final String ID = "iframe:pdf2";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(SourceDocument aContext)
    {
        return false;
    }

    @Override
    public Component createView(String aId, IModel<SourceDocument> aDoc, String aEditorFactoryId)
    {
        return new PdfDocumentIFrameView(aId, aDoc, aEditorFactoryId);
    }
}
